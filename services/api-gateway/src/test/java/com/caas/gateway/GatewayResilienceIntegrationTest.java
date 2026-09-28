package com.caas.gateway;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

// Mesmo arcabouço de GatewayValidJwtRoutingIntegrationTest: cognito-local real
// (JWT de verdade, JWKS real), não mock — ver o racional completo lá. As duas
// rotas apontam para localhost:1 (ninguém escutando, mesmo padrão de "rota
// quebrada de propósito" já usado em outros testes) — falha de conexão real,
// não uma resposta HTTP de erro, para o Circuit Breaker de fato registrar falha.
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
class GatewayResilienceIntegrationTest {

    @Container
    static GenericContainer<?> cognitoLocal =
        new GenericContainer<>(DockerImageName.parse("jagregory/cognito-local:latest"))
            .withExposedPorts(9229)
            .waitingFor(Wait.forLogMessage(".*Cognito Local running.*\\n", 1));

    private static final HttpClient HTTP_CLIENT = HttpClient.newHttpClient();
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    // Identidades separadas por teste para não uma consumir a cota de rate limit
    // da outra — o rate limiter é global à identidade, não por rota.
    private static String tenantAToken;
    private static String tenantBToken;
    private static String tenantCToken;

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) throws IOException, InterruptedException {
        String baseUrl = "http://" + cognitoLocal.getHost() + ":" + cognitoLocal.getMappedPort(9229);

        String poolId = cognito(baseUrl, "CreateUserPool", Map.of("PoolName", "test-pool"))
            .path("UserPool").path("Id").asText();
        String clientId = cognito(baseUrl, "CreateUserPoolClient", Map.of(
            "UserPoolId", poolId,
            "ClientName", "test-client",
            "ExplicitAuthFlows", List.of("ALLOW_ADMIN_USER_PASSWORD_AUTH", "ALLOW_REFRESH_TOKEN_AUTH")
        )).path("UserPoolClient").path("ClientId").asText();

        tenantAToken = createUserAndGetToken(baseUrl, poolId, clientId, "tenant-a@example.com");
        tenantBToken = createUserAndGetToken(baseUrl, poolId, clientId, "tenant-b@example.com");
        tenantCToken = createUserAndGetToken(baseUrl, poolId, clientId, "tenant-c@example.com");

        registry.add("app.cognito.jwk-set-uri", () -> baseUrl + "/" + poolId + "/.well-known/jwks.json");
        registry.add("app.routes.tenant-service-uri", () -> "http://localhost:1");
        registry.add("app.routes.proposal-service-uri", () -> "http://localhost:1");
    }

    private static String createUserAndGetToken(String baseUrl, String poolId, String clientId, String username)
        throws IOException, InterruptedException {
        cognito(baseUrl, "AdminCreateUser", Map.of(
            "UserPoolId", poolId, "Username", username,
            "TemporaryPassword", "Temp1234!", "MessageAction", "SUPPRESS"
        ));
        cognito(baseUrl, "AdminSetUserPassword", Map.of(
            "UserPoolId", poolId, "Username", username, "Password", "Passw0rd1!", "Permanent", true
        ));
        JsonNode auth = cognito(baseUrl, "AdminInitiateAuth", Map.of(
            "UserPoolId", poolId, "ClientId", clientId, "AuthFlow", "ADMIN_USER_PASSWORD_AUTH",
            "AuthParameters", Map.of("USERNAME", username, "PASSWORD", "Passw0rd1!")
        ));
        return auth.path("AuthenticationResult").path("IdToken").asText();
    }

    private static JsonNode cognito(String baseUrl, String target, Map<String, Object> body)
        throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder(URI.create(baseUrl))
            .header("Content-Type", "application/x-amz-json-1.1")
            .header("X-Amz-Target", "AWSCognitoIdentityProviderService." + target)
            .POST(HttpRequest.BodyPublishers.ofString(OBJECT_MAPPER.writeValueAsString(body)))
            .build();
        HttpResponse<String> response = HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofString());
        return OBJECT_MAPPER.readTree(response.body());
    }

    @Autowired
    private WebTestClient webTestClient;

    private HttpStatus statusOf(String path, String token) {
        return HttpStatus.valueOf(webTestClient.get().uri(path)
            .header("Authorization", "Bearer " + token)
            .exchange()
            .returnResult(String.class)
            .getStatus()
            .value());
    }

    @Test
    void afterRepeatedDownstreamFailuresTheRouteCircuitOpensAndFailsFast() {
        HttpStatus lastStatus = null;
        // minimum-number-of-calls é 5; bem dentro da cota de rate limit (15) desta
        // identidade isolada, então o Circuit Breaker é o único fator em jogo aqui.
        for (int i = 0; i < 9; i++) {
            lastStatus = statusOf("/tenants/00000000-0000-0000-0000-000000000000", tenantCToken);
        }

        assertThat(lastStatus).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
    }

    @Test
    void aRateLimitedIdentityDoesNotAffectAnotherIdentity() {
        HttpStatus lastStatus = null;
        // Bem além da cota de rate limit (15) desta identidade — as últimas
        // chamadas devem ser barradas pelo rate limiter, não pelo Circuit Breaker.
        for (int i = 0; i < 20; i++) {
            lastStatus = statusOf("/proposals/00000000-0000-0000-0000-000000000000", tenantAToken);
        }
        assertThat(lastStatus).isEqualTo(HttpStatus.TOO_MANY_REQUESTS);

        HttpStatus tenantBStatus = statusOf("/proposals/00000000-0000-0000-0000-000000000000", tenantBToken);
        assertThat(tenantBStatus).isNotEqualTo(HttpStatus.TOO_MANY_REQUESTS);
    }
}
