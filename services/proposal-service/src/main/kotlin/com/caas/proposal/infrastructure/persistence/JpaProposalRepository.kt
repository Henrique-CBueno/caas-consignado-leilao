package com.caas.proposal.infrastructure.persistence

import com.caas.proposal.application.ProposalRepository
import com.caas.proposal.domain.Proposal
import com.caas.proposal.domain.ProposalId
import com.caas.proposal.domain.ProposalStatus
import com.caas.proposal.domain.TenantId
import com.caas.proposal.infrastructure.TenantContextHolder
import jakarta.persistence.Entity
import jakarta.persistence.EntityManager
import jakarta.persistence.Id
import jakarta.persistence.Table
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.util.UUID

@Entity
@Table(name = "proposals")
class ProposalJpaEntity(
    @Id
    val id: UUID,
    val tenantId: UUID,
    val borrowerId: String,
    val requestedAmount: BigDecimal,
    val termMonths: Int,
    val status: String,
) {
    protected constructor() : this(UUID(0, 0), UUID(0, 0), "", BigDecimal.ZERO, 0, "")
}

interface SpringDataProposalRepository : JpaRepository<ProposalJpaEntity, UUID>

@Repository
class JpaProposalRepository(
    private val springDataRepository: SpringDataProposalRepository,
    private val entityManager: EntityManager,
) : ProposalRepository {

    @Transactional
    override fun save(proposal: Proposal): Proposal {
        applyTenantContext()
        val saved =
            springDataRepository.save(
                ProposalJpaEntity(
                    id = proposal.id.value,
                    tenantId = proposal.tenantId.value,
                    borrowerId = proposal.borrowerId,
                    requestedAmount = proposal.requestedAmount,
                    termMonths = proposal.termMonths,
                    status = proposal.status.name,
                ),
            )
        return saved.toDomain()
    }

    @Transactional
    override fun findById(id: ProposalId): Proposal? {
        applyTenantContext()
        return springDataRepository.findById(id.value).map { it.toDomain() }.orElse(null)
    }

    // Ver JpaTenantRepository (tenant-service) para o racional completo: sem isso,
    // o login superusuário do Testcontainers ignora RLS mesmo com FORCE.
    private fun applyTenantContext() {
        entityManager.createNativeQuery("SET LOCAL ROLE app_role").executeUpdate()
        val tenantId = TenantContextHolder.get() ?: return
        entityManager
            .createNativeQuery("SELECT set_config('app.current_tenant', :tenantId, true)")
            .setParameter("tenantId", tenantId.value.toString())
            .singleResult
    }

    private fun ProposalJpaEntity.toDomain() =
        Proposal(
            id = ProposalId(id),
            tenantId = TenantId(tenantId),
            borrowerId = borrowerId,
            requestedAmount = requestedAmount,
            termMonths = termMonths,
            status = ProposalStatus.valueOf(status),
        )
}
