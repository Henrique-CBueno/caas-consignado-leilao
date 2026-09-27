package com.caas.gateway;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

// Cognito real via LocalStack não é viável: o LocalStack Community não implementa
// cognito-idp de forma alguma (confirmado empiricamente na Milestone 1 — ver ADR-0002).
// cognito-local (jagregory/cognito-local) é usado aqui: emulador dedicado, gratuito,
// que assina JWTs de verdade com JWKS real via a mesma API JSON do Cognito.
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
class GatewayValidJwtRoutingIntegrationTest {

    @Container
    static GenericContainer<?> cognitoLocal =
        new GenericContainer<>(DockerImageName.parse("jagregory/cognito-local:latest"))
            .withExposedPorts(9229)
            .waitingFor(Wait.forLogMessage(".*Cognito Local running.*\\n", 1));

    private static final HttpServer DOWNSTREAM = createDownstream();
    private static final HttpClient HTTP_CLIENT = HttpClient.newHttpClient();
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private static String idToken;

    private static HttpServer createDownstream() {
        try {
            HttpServer server = HttpServer.create(new InetSocketAddress(0), 0);
            server.createContext("/tenants", exchange -> {
                byte[] body = "ok".getBytes();
                exchange.sendResponseHeaders(200, body.length);
                try (var os = exchange.getResponseBody()) {
                    os.write(body);
                }
            });
            server.start();
            return server;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @AfterAll
    static void tearDown() {
        DOWNSTREAM.stop(0);
    }

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

        cognito(baseUrl, "AdminCreateUser", Map.of(
            "UserPoolId", poolId,
            "Username", "tenant-a@example.com",
            "TemporaryPassword", "Temp1234!",
            "MessageAction", "SUPPRESS"
        ));
        cognito(baseUrl, "AdminSetUserPassword", Map.of(
            "UserPoolId", poolId,
            "Username", "tenant-a@example.com",
            "Password", "Passw0rd1!",
            "Permanent", true
        ));
        JsonNode auth = cognito(baseUrl, "AdminInitiateAuth", Map.of(
            "UserPoolId", poolId,
            "ClientId", clientId,
            "AuthFlow", "ADMIN_USER_PASSWORD_AUTH",
            "AuthParameters", Map.of("USERNAME", "tenant-a@example.com", "PASSWORD", "Passw0rd1!")
        ));
        idToken = auth.path("AuthenticationResult").path("IdToken").asText();

        registry.add("app.cognito.jwk-set-uri", () -> baseUrl + "/" + poolId + "/.well-known/jwks.json");
        registry.add("app.routes.proposal-service-uri", () -> "http://localhost:1");
        registry.add("app.routes.tenant-service-uri", () -> "http://localhost:" + DOWNSTREAM.getAddress().getPort());
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

    @Test
    void aRequestWithAValidCognitoIssuedTokenIsRoutedThrough() {
        webTestClient.get().uri("/tenants/00000000-0000-0000-0000-000000000000")
            .header("Authorization", "Bearer " + idToken)
            .exchange()
            .expectStatus().isOk();
    }
}
