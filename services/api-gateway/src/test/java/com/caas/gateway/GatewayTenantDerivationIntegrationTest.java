package com.caas.gateway;

import static org.assertj.core.api.Assertions.assertThat;

import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
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

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
class GatewayTenantDerivationIntegrationTest {

    private static final String TENANT_A = "11111111-1111-1111-1111-111111111111";
    private static final String TENANT_B = "22222222-2222-2222-2222-222222222222";
    private static final String TENANT_C = "33333333-3333-3333-3333-333333333333";
    private static final String TENANT_D = "44444444-4444-4444-4444-444444444444";

    @Container
    static GenericContainer<?> cognitoLocal =
        new GenericContainer<>(DockerImageName.parse("jagregory/cognito-local:latest"))
            .withExposedPorts(9229)
            .waitingFor(Wait.forLogMessage(".*Cognito Local running.*\\n", 1));

    // Cada elemento é o X-Tenant-Id que o serviço de destino recebeu ("null" se veio sem o header).
    static final List<String> receivedTenantHeaders = new CopyOnWriteArrayList<>();
    static final HttpServer DOWNSTREAM = createDownstream();
    static CognitoLocalFixture cognito;

    private static HttpServer createDownstream() {
        try {
            HttpServer server = HttpServer.create(new InetSocketAddress(0), 0);
            server.createContext("/", exchange -> {
                receivedTenantHeaders.add(String.valueOf(exchange.getRequestHeaders().getFirst("X-Tenant-Id")));
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
    static void properties(DynamicPropertyRegistry registry) throws Exception {
        cognito = new CognitoLocalFixture("http://" + cognitoLocal.getHost() + ":" + cognitoLocal.getMappedPort(9229));
        registry.add("app.cognito.jwk-set-uri", cognito::jwkSetUri);
        registry.add("app.routes.tenant-service-uri", () -> "http://localhost:" + DOWNSTREAM.getAddress().getPort());
        registry.add("app.routes.proposal-service-uri", () -> "http://localhost:" + DOWNSTREAM.getAddress().getPort());
        registry.add("app.routes.disbursement-service-uri", () -> "http://localhost:" + DOWNSTREAM.getAddress().getPort());
    }

    @Autowired
    WebTestClient webTestClient;

    @BeforeEach
    void clearReceived() {
        receivedTenantHeaders.clear();
    }

    @Test
    void theServiceReceivesTheTenantFromTheTokenClaim() throws Exception {
        String token = cognito.idTokenFor("user-a@example.com", TENANT_A);

        webTestClient.get().uri("/proposals/abc")
            .header("Authorization", "Bearer " + token)
            .exchange()
            .expectStatus().isOk();

        assertThat(receivedTenantHeaders).containsExactly(TENANT_A);
    }

    @Test
    void aTenantHeaderForgedByTheClientIsReplacedByTheTokenClaim() throws Exception {
        String token = cognito.idTokenFor("user-forger@example.com", TENANT_A);

        webTestClient.get().uri("/proposals/abc")
            .header("Authorization", "Bearer " + token)
            .header("X-Tenant-Id", TENANT_B)
            .exchange()
            .expectStatus().isOk();

        assertThat(receivedTenantHeaders).containsExactly(TENANT_A);
    }

    @Test
    void aValidTokenWithoutATenantClaimIsForbiddenAndNeverReachesTheService() throws Exception {
        String token = cognito.idTokenFor("user-no-tenant@example.com", null);

        webTestClient.get().uri("/proposals/abc")
            .header("Authorization", "Bearer " + token)
            .exchange()
            .expectStatus().isForbidden();

        assertThat(receivedTenantHeaders).isEmpty();
    }

    @Test
    void aTenantClaimThatIsNotAUuidIsForbiddenAndNeverReachesTheService() throws Exception {
        String token = cognito.idTokenFor("user-bad-tenant@example.com", "not-a-uuid");

        webTestClient.get().uri("/proposals/abc")
            .header("Authorization", "Bearer " + token)
            .exchange()
            .expectStatus().isForbidden();

        assertThat(receivedTenantHeaders).isEmpty();
    }

    @Test
    void disbursementsAreRoutedThroughTheGatewayWithTheTenantFromTheToken() throws Exception {
        String token = cognito.idTokenFor("user-disb@example.com", TENANT_A);

        webTestClient.get().uri("/disbursements/abc")
            .header("Authorization", "Bearer " + token)
            .exchange()
            .expectStatus().isOk();

        assertThat(receivedTenantHeaders).containsExactly(TENANT_A);
    }

    private int statusOf(String token) {
        return webTestClient.get().uri("/proposals/abc")
            .header("Authorization", "Bearer " + token)
            .exchange()
            .returnResult(String.class)
            .getStatus()
            .value();
    }

    @Test
    void theRateLimitIsSharedByIdentitiesOfTheSameTenantAndNotByOtherTenants() throws Exception {
        String firstUser = cognito.idTokenFor("rate-1@example.com", TENANT_C);
        String secondUser = cognito.idTokenFor("rate-2@example.com", TENANT_C);
        String otherTenantUser = cognito.idTokenFor("rate-3@example.com", TENANT_D);

        // Limite base de 15 requisições por janela: alternar os dois usuários do mesmo tenant esgota a cota comum.
        for (int i = 0; i < 15; i++) {
            assertThat(statusOf(i % 2 == 0 ? firstUser : secondUser)).isEqualTo(200);
        }

        assertThat(statusOf(firstUser)).isEqualTo(429);
        assertThat(statusOf(secondUser)).isEqualTo(429);
        assertThat(statusOf(otherTenantUser)).isEqualTo(200);
    }
}
