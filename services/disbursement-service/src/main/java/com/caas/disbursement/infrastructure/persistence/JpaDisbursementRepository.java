package com.caas.disbursement.infrastructure.persistence;

import com.caas.disbursement.application.DisbursementRepository;
import com.caas.disbursement.domain.Disbursement;
import com.caas.disbursement.domain.ProposalId;
import com.caas.disbursement.domain.TenantId;
import com.caas.disbursement.infrastructure.TenantContextHolder;
import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
public class JpaDisbursementRepository implements DisbursementRepository {

    private final SpringDataDisbursementRepository springDataRepository;
    private final EntityManager entityManager;

    public JpaDisbursementRepository(SpringDataDisbursementRepository springDataRepository, EntityManager entityManager) {
        this.springDataRepository = springDataRepository;
        this.entityManager = entityManager;
    }

    @Override
    @Transactional
    public Disbursement save(Disbursement disbursement) {
        applyTenantContext();
        DisbursementJpaEntity saved = springDataRepository.save(new DisbursementJpaEntity(
            disbursement.proposalId().value(),
            disbursement.tenantId().value(),
            disbursement.funderId(),
            disbursement.amount(),
            disbursement.status(),
            disbursement.disbursedAt()
        ));
        return toDomain(saved);
    }

    @Override
    @Transactional
    public Disbursement findByProposalId(ProposalId proposalId) {
        applyTenantContext();
        return springDataRepository.findById(proposalId.value()).map(JpaDisbursementRepository::toDomain).orElse(null);
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

    private static Disbursement toDomain(DisbursementJpaEntity entity) {
        return new Disbursement(
            new ProposalId(entity.getProposalId()),
            new TenantId(entity.getTenantId()),
            entity.getFunderId(),
            entity.getAmount(),
            entity.getStatus(),
            entity.getDisbursedAt()
        );
    }
}
