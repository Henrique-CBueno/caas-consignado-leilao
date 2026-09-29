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
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

// Milestone 17: papel administrativo (claim custom:role=admin) só abre /admin/**; nunca dados de tenant.
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
class GatewayAdminAccessIntegrationTest {

    @Container
    static GenericContainer<?> cognitoLocal =
        new GenericContainer<>(DockerImageName.parse("jagregory/cognito-local:latest"))
            .withExposedPorts(9229)
            .waitingFor(Wait.forLogMessage(".*Cognito Local running.*\\n", 1));

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final HttpServer TENANT_SERVICE_STUB = stub("/admin/tenants");
    private static final HttpServer PROPOSAL_SERVICE_STUB = stub("/proposals");

    private static HttpServer stub(String path) {
        try {
            HttpServer server = HttpServer.create(new InetSocketAddress(0), 0);
            server.createContext(path, exchange -> {
                byte[] body = "[]".getBytes();
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

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) throws Exception {
        CognitoLocalFixture cognito =
            new CognitoLocalFixture("http://" + cognitoLocal.getHost() + ":" + cognitoLocal.getMappedPort(9229));
        cognito.createUser("alfa@caas.local", "11111111-1111-1111-1111-111111111111");
        cognito.createAdmin("admin@caas.local");

        registry.add("app.cognito.jwk-set-uri", cognito::jwkSetUri);
        registry.add("app.cognito.base-uri", cognito::baseUri);
        registry.add("app.cognito.client-id", cognito::clientId);
        registry.add("app.routes.tenant-service-uri", () -> "http://localhost:" + TENANT_SERVICE_STUB.getAddress().getPort());
        registry.add("app.routes.proposal-service-uri", () -> "http://localhost:" + PROPOSAL_SERVICE_STUB.getAddress().getPort());
        registry.add("app.routes.disbursement-service-uri", () -> "http://localhost:1");
    }

    @Autowired
    private WebTestClient webTestClient;

    private String login(String tenant) throws Exception {
        String body = webTestClient.post().uri("/auth/login")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue("{\"tenant\":\"" + tenant + "\"}")
            .exchange().expectStatus().isOk()
            .expectBody(String.class).returnResult().getResponseBody();
        return OBJECT_MAPPER.readTree(body).path("idToken").asText();
    }

    @Test
    void theAdminIdentityCanLogInAndCarriesTheAdminRoleClaim() throws Exception {
        String payload = login("admin").split("\\.")[1];
        payload += "=".repeat((4 - payload.length() % 4) % 4);
        JsonNode claims = OBJECT_MAPPER.readTree(Base64.getUrlDecoder().decode(payload));

        assertThat(claims.path("custom:role").asText()).isEqualTo("admin");
    }

    @Test
    void theAdminIdentityReachesTheAdminTenantsRoute() throws Exception {
        webTestClient.get().uri("/admin/tenants")
            .header("Authorization", "Bearer " + login("admin"))
            .exchange().expectStatus().isOk();
    }

    @Test
    void aCommonTenantIdentityIsForbiddenOnTheAdminRoute() throws Exception {
        webTestClient.get().uri("/admin/tenants")
            .header("Authorization", "Bearer " + login("alfa"))
            .exchange().expectStatus().isForbidden();
    }

    @Test
    void theAdminIdentityIsForbiddenOnTenantData() throws Exception {
        webTestClient.get().uri("/proposals/x")
            .header("Authorization", "Bearer " + login("admin"))
            .exchange().expectStatus().isForbidden();
    }

    @Test
    void theAdminRouteRequiresAToken() {
        webTestClient.get().uri("/admin/tenants").exchange().expectStatus().isUnauthorized();
    }
}
