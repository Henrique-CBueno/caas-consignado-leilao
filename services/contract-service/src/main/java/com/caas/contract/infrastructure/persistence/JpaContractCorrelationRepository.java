package com.caas.contract.infrastructure.persistence;

import com.caas.contract.application.ContractCorrelationRepository;
import com.caas.contract.domain.ContractCorrelation;
import com.caas.contract.domain.ProposalId;
import com.caas.contract.domain.TenantId;
import com.caas.contract.infrastructure.TenantContextHolder;
import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
public class JpaContractCorrelationRepository implements ContractCorrelationRepository {

    private final SpringDataContractCorrelationRepository springDataRepository;
    private final EntityManager entityManager;

    public JpaContractCorrelationRepository(
        SpringDataContractCorrelationRepository springDataRepository, EntityManager entityManager
    ) {
        this.springDataRepository = springDataRepository;
        this.entityManager = entityManager;
    }

    @Override
    @Transactional
    public ContractCorrelation findByProposalIdForUpdate(ProposalId proposalId) {
        applyTenantContext();
        ContractCorrelationJpaEntity entity = springDataRepository.findByProposalId(proposalId.value());
        return entity == null ? null : toDomain(entity);
    }

    @Override
    @Transactional
    public ContractCorrelation save(ContractCorrelation correlation) {
        applyTenantContext();
        ContractCorrelationJpaEntity saved = springDataRepository.save(new ContractCorrelationJpaEntity(
            correlation.proposalId().value(),
            correlation.tenantId().value(),
            correlation.requestedAmount(),
            correlation.winningFunderId(),
            correlation.winningRate(),
            correlation.winningTermMonths(),
            correlation.contractGenerated()
        ));
        return toDomain(saved);
    }

    // Ver JpaTenantRepository (tenant-service) para o racional completo: sem isso,
    // o login superusuário do Testcontainers ignora RLS mesmo com FORCE.
    private void applyTenantContext() {
        entityManager.createNativeQuery("SET LOCAL ROLE app_role").executeUpdate();
        TenantId tenantId = TenantContextHolder.get();
        if (tenantId == null) {
            return;
        }
        entityManager
            .createNativeQuery("SELECT set_config('app.current_tenant', :tenantId, true)")
            .setParameter("tenantId", tenantId.value().toString())
            .getSingleResult();
    }

    private static ContractCorrelation toDomain(ContractCorrelationJpaEntity entity) {
        return new ContractCorrelation(
            new ProposalId(entity.getProposalId()),
            new TenantId(entity.getTenantId()),
            entity.getRequestedAmount(),
            entity.getWinningFunderId(),
            entity.getWinningRate(),
            entity.getWinningTermMonths(),
            entity.isContractGenerated()
        );
    }
}
