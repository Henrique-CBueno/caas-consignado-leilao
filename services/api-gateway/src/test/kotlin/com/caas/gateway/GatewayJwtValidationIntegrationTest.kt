package com.caas.gateway

import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.HttpStatus
import org.springframework.test.web.reactive.server.WebTestClient

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class GatewayJwtValidationIntegrationTest {

    @Autowired
    lateinit var webTestClient: WebTestClient

    @Test
    fun `a request without a token is rejected`() {
        webTestClient.get().uri("/tenants/00000000-0000-0000-0000-000000000000")
            .exchange()
            .expectStatus().isEqualTo(HttpStatus.UNAUTHORIZED)
    }

    @Test
    fun `a request with a malformed token is rejected`() {
        webTestClient.get().uri("/tenants/00000000-0000-0000-0000-000000000000")
            .header("Authorization", "Bearer not-a-real-jwt")
            .exchange()
            .expectStatus().isEqualTo(HttpStatus.UNAUTHORIZED)
    }
}
