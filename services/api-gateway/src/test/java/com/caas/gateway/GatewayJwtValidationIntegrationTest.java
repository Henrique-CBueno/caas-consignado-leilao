package com.caas.gateway;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.reactive.server.WebTestClient;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class GatewayJwtValidationIntegrationTest {

    @Autowired
    private WebTestClient webTestClient;

    @Test
    void aRequestWithoutATokenIsRejected() {
        webTestClient.get().uri("/tenants/00000000-0000-0000-0000-000000000000")
            .exchange()
            .expectStatus().isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void aRequestWithAMalformedTokenIsRejected() {
        webTestClient.get().uri("/tenants/00000000-0000-0000-0000-000000000000")
            .header("Authorization", "Bearer not-a-real-jwt")
            .exchange()
            .expectStatus().isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void actuatorHealthIsReachableWithoutATokenSoPrometheusCanScrapeTheGateway() {
        webTestClient.get().uri("/actuator/health")
            .exchange()
            .expectStatus().isOk();
    }
}
