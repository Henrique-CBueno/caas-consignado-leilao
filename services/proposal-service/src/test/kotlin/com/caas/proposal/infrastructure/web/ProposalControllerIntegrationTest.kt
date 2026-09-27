package com.caas.proposal.infrastructure.web

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.client.TestRestTemplate
import org.springframework.boot.test.web.server.LocalServerPort
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import java.math.BigDecimal
import java.util.UUID
import org.springframework.beans.factory.annotation.Autowired

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
class ProposalControllerIntegrationTest {

    @LocalServerPort
    var port: Int = 0

    @Autowired
    lateinit var restTemplate: TestRestTemplate

    private fun headers(tenantId: UUID) = HttpHeaders().apply { set("X-Tenant-Id", tenantId.toString()) }

    private fun createProposal(tenantId: UUID): CreateProposalResponse {
        val request = CreateProposalRequest(borrowerId = "12345678900", requestedAmount = BigDecimal("5000.00"), termMonths = 24)
        val response = restTemplate.exchange(
            "http://localhost:$port/proposals",
            HttpMethod.POST,
            HttpEntity(request, headers(tenantId)),
            CreateProposalResponse::class.java,
        )
        return response.body!!
    }

    @Test
    fun `a tenant can create a proposal and retrieve it back`() {
        val tenantId = UUID.randomUUID()

        val created = createProposal(tenantId)

        val response = restTemplate.exchange(
            "http://localhost:$port/proposals/${created.id}",
            HttpMethod.GET,
            HttpEntity<Void>(headers(tenantId)),
            CreateProposalResponse::class.java,
        )

        assertThat(response.statusCode).isEqualTo(HttpStatus.OK)
        assertThat(response.body?.borrowerId).isEqualTo("12345678900")
        assertThat(response.body?.status).isEqualTo("PENDING_CREDIT_ANALYSIS")
    }

    @Test
    fun `creating a proposal with a non-positive requested amount is rejected`() {
        val request = CreateProposalRequest(borrowerId = "12345678900", requestedAmount = BigDecimal.ZERO, termMonths = 24)

        val response = restTemplate.exchange(
            "http://localhost:$port/proposals",
            HttpMethod.POST,
            HttpEntity(request, headers(UUID.randomUUID())),
            String::class.java,
        )

        assertThat(response.statusCode).isEqualTo(HttpStatus.BAD_REQUEST)
    }

    @Test
    fun `a tenant cannot retrieve another tenant's proposal`() {
        val tenantA = UUID.randomUUID()
        val tenantB = UUID.randomUUID()
        val created = createProposal(tenantA)

        val response = restTemplate.exchange(
            "http://localhost:$port/proposals/${created.id}",
            HttpMethod.GET,
            HttpEntity<Void>(headers(tenantB)),
            String::class.java,
        )

        assertThat(response.statusCode).isEqualTo(HttpStatus.NOT_FOUND)
    }

    companion object {
        @Container
        @JvmStatic
        val postgres = PostgreSQLContainer("postgres:16-alpine")

        @DynamicPropertySource
        @JvmStatic
        fun properties(registry: DynamicPropertyRegistry) {
            registry.add("spring.datasource.url", postgres::getJdbcUrl)
            registry.add("spring.datasource.username", postgres::getUsername)
            registry.add("spring.datasource.password", postgres::getPassword)
        }
    }
}
