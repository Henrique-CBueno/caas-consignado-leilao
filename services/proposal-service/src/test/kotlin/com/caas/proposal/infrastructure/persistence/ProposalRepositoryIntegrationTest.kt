package com.caas.proposal.infrastructure.persistence

import com.caas.proposal.application.ProposalRepository
import com.caas.proposal.domain.Proposal
import com.caas.proposal.domain.ProposalId
import com.caas.proposal.domain.TenantId
import com.caas.proposal.infrastructure.TenantContextHolder
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import java.math.BigDecimal
import java.util.UUID

@SpringBootTest
@Testcontainers
class ProposalRepositoryIntegrationTest {

    @Autowired
    lateinit var proposalRepository: ProposalRepository

    @AfterEach
    fun tearDown() {
        TenantContextHolder.clear()
    }

    private fun aProposal(tenantId: TenantId) = Proposal(
        id = ProposalId(UUID.randomUUID()),
        tenantId = tenantId,
        borrowerId = "12345678900",
        requestedAmount = BigDecimal("5000.00"),
        termMonths = 24,
    )

    @Test
    fun `a tenant session cannot read another tenant's proposal by id`() {
        val tenantA = TenantId(UUID.randomUUID())
        val tenantB = TenantId(UUID.randomUUID())
        val proposalOfA = aProposal(tenantA)

        TenantContextHolder.set(tenantA)
        proposalRepository.save(proposalOfA)

        TenantContextHolder.set(tenantB)
        val leaked = proposalRepository.findById(proposalOfA.id)

        assertThat(leaked).isNull()
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
