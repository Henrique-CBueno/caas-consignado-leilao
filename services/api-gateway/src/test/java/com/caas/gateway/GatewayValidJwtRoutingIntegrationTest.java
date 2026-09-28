package com.caas.gateway;

import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.InetSocketAddress;
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
    static void properties(DynamicPropertyRegistry registry) throws Exception {
        CognitoLocalFixture cognito =
            new CognitoLocalFixture("http://" + cognitoLocal.getHost() + ":" + cognitoLocal.getMappedPort(9229));
        idToken = cognito.idTokenFor("tenant-a@example.com", "11111111-1111-1111-1111-111111111111");

        registry.add("app.cognito.jwk-set-uri", cognito::jwkSetUri);
        registry.add("app.routes.proposal-service-uri", () -> "http://localhost:1");
        registry.add("app.routes.tenant-service-uri", () -> "http://localhost:" + DOWNSTREAM.getAddress().getPort());
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
