package com.caas.gateway;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.Base64;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

// Rota pública (sem JWT) que autentica um dos tenants de demonstração contra o cognito-local
// pelo fluxo público (USER_PASSWORD_AUTH), nunca pelo administrativo (Milestone 16).
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
class GatewayLoginIntegrationTest {

    @Container
    static GenericContainer<?> cognitoLocal =
        new GenericContainer<>(DockerImageName.parse("jagregory/cognito-local:latest"))
            .withExposedPorts(9229)
            .waitingFor(Wait.forLogMessage(".*Cognito Local running.*\\n", 1));

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private static final HttpServer PROPOSAL_SERVICE_STUB = createProposalServiceStub();

    private static HttpServer createProposalServiceStub() {
        try {
            HttpServer server = HttpServer.create(new InetSocketAddress(0), 0);
            server.createContext("/proposals", exchange -> {
                byte[] body = "ok".getBytes();
                exchange.sendResponseHeaders(201, body.length);
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

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) throws Exception {
        CognitoLocalFixture cognito =
            new CognitoLocalFixture("http://" + cognitoLocal.getHost() + ":" + cognitoLocal.getMappedPort(9229));
        cognito.createUser("alfa@caas.local", "11111111-1111-1111-1111-111111111111");
        cognito.createUser("banco-omega@caas.local", "44444444-4444-4444-4444-444444444444");

        registry.add("app.cognito.jwk-set-uri", cognito::jwkSetUri);
        registry.add("app.cognito.base-uri", cognito::baseUri);
        registry.add("app.cognito.client-id", cognito::clientId);
        registry.add("app.routes.tenant-service-uri", () -> "http://localhost:1");
        registry.add("app.routes.proposal-service-uri",
            () -> "http://localhost:" + PROPOSAL_SERVICE_STUB.getAddress().getPort());
        registry.add("app.routes.disbursement-service-uri", () -> "http://localhost:1");
    }

    @Autowired
    private WebTestClient webTestClient;

    @Test
    void loggingInAsADemoTenantReturnsATokenCarryingThatTenantsClaim() throws Exception {
        String body = webTestClient.post().uri("/auth/login")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue("""
                {"tenant":"alfa"}
                """)
            .exchange()
            .expectStatus().isOk()
            .expectBody(String.class)
            .returnResult()
            .getResponseBody();

        JsonNode json = OBJECT_MAPPER.readTree(body);
        String idToken = json.path("idToken").asText();
        assertThat(idToken).isNotBlank();
        assertThat(claim(idToken, "custom:tenant_id")).isEqualTo("11111111-1111-1111-1111-111111111111");
    }

    @Test
    void aTenantCreatedLaterByTheAdminCanAlsoLogIn() throws Exception {
        String body = webTestClient.post().uri("/auth/login")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue("{\"tenant\":\"banco-omega\"}")
            .exchange()
            .expectStatus().isOk()
            .expectBody(String.class)
            .returnResult()
            .getResponseBody();

        assertThat(claim(OBJECT_MAPPER.readTree(body).path("idToken").asText(), "custom:tenant_id"))
            .isEqualTo("44444444-4444-4444-4444-444444444444");
    }

    private static String claim(String jwt, String claim) throws Exception {
        String payload = jwt.split("\\.")[1];
        payload += "=".repeat((4 - payload.length() % 4) % 4);
        JsonNode json = OBJECT_MAPPER.readTree(Base64.getUrlDecoder().decode(payload));
        return json.path(claim).asText();
    }

    @Test
    void loggingInWithAnUnknownTenantIsRejected() {
        webTestClient.post().uri("/auth/login")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue("""
                {"tenant":"nao-existe"}
                """)
            .exchange()
            .expectStatus().isBadRequest();
    }

    @Test
    void theTokenReturnedByLoginIsAcceptedByARealCreateProposalCall() throws Exception {
        String body = webTestClient.post().uri("/auth/login")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue("""
                {"tenant":"alfa"}
                """)
            .exchange()
            .expectBody(String.class)
            .returnResult()
            .getResponseBody();
        String idToken = OBJECT_MAPPER.readTree(body).path("idToken").asText();

        webTestClient.post().uri("/proposals")
            .header("Authorization", "Bearer " + idToken)
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue("""
                {"borrowerId":"59","requestedAmount":5000.00,"termMonths":24}
                """)
            .exchange()
            .expectStatus().isCreated();
    }

    @Test
    void loginAndCreateProposalAllowThePreflightFromTheDashboardOrigin() {
        webTestClient.options().uri("/auth/login")
            .header("Origin", "http://localhost:30090")
            .header("Access-Control-Request-Method", "POST")
            .exchange()
            .expectStatus().isOk()
            .expectHeader().exists("Access-Control-Allow-Origin");

        webTestClient.options().uri("/proposals")
            .header("Origin", "http://localhost:30090")
            .header("Access-Control-Request-Method", "POST")
            .exchange()
            .expectStatus().isOk()
            .expectHeader().exists("Access-Control-Allow-Origin");
    }
}
