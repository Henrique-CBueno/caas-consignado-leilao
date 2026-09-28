package com.caas.gateway;

import static org.assertj.core.api.Assertions.assertThat;

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

    // Tenants separados por teste para um não consumir a cota de rate limit do
    // outro — o rate limiter é por tenant, não por rota (ADR-0021).
    private static String tenantAToken;
    private static String tenantBToken;
    private static String tenantCToken;

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) throws Exception {
        CognitoLocalFixture cognito =
            new CognitoLocalFixture("http://" + cognitoLocal.getHost() + ":" + cognitoLocal.getMappedPort(9229));

        tenantAToken = cognito.idTokenFor("tenant-a@example.com", "11111111-1111-1111-1111-111111111111");
        tenantBToken = cognito.idTokenFor("tenant-b@example.com", "22222222-2222-2222-2222-222222222222");
        tenantCToken = cognito.idTokenFor("tenant-c@example.com", "33333333-3333-3333-3333-333333333333");

        registry.add("app.cognito.jwk-set-uri", cognito::jwkSetUri);
        registry.add("app.routes.tenant-service-uri", () -> "http://localhost:1");
        registry.add("app.routes.proposal-service-uri", () -> "http://localhost:1");
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
        // minimum-number-of-calls é 5; bem dentro da cota de rate limit (15) deste
        // tenant isolado, então o Circuit Breaker é o único fator em jogo aqui.
        for (int i = 0; i < 9; i++) {
            lastStatus = statusOf("/tenants/00000000-0000-0000-0000-000000000000", tenantCToken);
        }

        assertThat(lastStatus).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
    }

    @Test
    void aRateLimitedTenantDoesNotAffectAnotherTenant() {
        HttpStatus lastStatus = null;
        // Bem além da cota de rate limit (15) deste tenant — as últimas
        // chamadas devem ser barradas pelo rate limiter, não pelo Circuit Breaker.
        for (int i = 0; i < 20; i++) {
            lastStatus = statusOf("/proposals/00000000-0000-0000-0000-000000000000", tenantAToken);
        }
        assertThat(lastStatus).isEqualTo(HttpStatus.TOO_MANY_REQUESTS);

        HttpStatus tenantBStatus = statusOf("/proposals/00000000-0000-0000-0000-000000000000", tenantBToken);
        assertThat(tenantBStatus).isNotEqualTo(HttpStatus.TOO_MANY_REQUESTS);
    }
}
