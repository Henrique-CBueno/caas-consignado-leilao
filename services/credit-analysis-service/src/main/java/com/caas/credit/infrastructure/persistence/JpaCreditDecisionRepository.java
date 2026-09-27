package com.caas.credit.infrastructure.persistence;

import com.caas.credit.application.CreditDecisionRepository;
import com.caas.credit.domain.CreditDecision;
import com.caas.credit.domain.CreditDecisionId;
import com.caas.credit.domain.Decision;
import com.caas.credit.domain.ProposalId;
import com.caas.credit.domain.TenantId;
import com.caas.credit.infrastructure.TenantContextHolder;
import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
public class JpaCreditDecisionRepository implements CreditDecisionRepository {

    private final SpringDataCreditDecisionRepository springDataRepository;
    private final EntityManager entityManager;

    public JpaCreditDecisionRepository(SpringDataCreditDecisionRepository springDataRepository, EntityManager entityManager) {
        this.springDataRepository = springDataRepository;
        this.entityManager = entityManager;
    }

    @Override
    @Transactional
    public CreditDecision save(CreditDecision creditDecision) {
        applyTenantContext();
        CreditDecisionJpaEntity saved = springDataRepository.save(new CreditDecisionJpaEntity(
            creditDecision.id().value(),
            creditDecision.proposalId().value(),
            creditDecision.tenantId().value(),
            creditDecision.decision().name(),
            creditDecision.confidence()
        ));
        return toDomain(saved);
    }

    @Override
    @Transactional
    public CreditDecision findById(CreditDecisionId id) {
        applyTenantContext();
        return springDataRepository.findById(id.value()).map(JpaCreditDecisionRepository::toDomain).orElse(null);
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

    private static CreditDecision toDomain(CreditDecisionJpaEntity entity) {
        return new CreditDecision(
            new CreditDecisionId(entity.getId()),
            new ProposalId(entity.getProposalId()),
            new TenantId(entity.getTenantId()),
            Decision.valueOf(entity.getDecision()),
            entity.getConfidence()
        );
    }
}
