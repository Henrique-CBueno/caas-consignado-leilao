package com.caas.contract.infrastructure.persistence;

import com.caas.contract.application.ContractRepository;
import com.caas.contract.domain.Contract;
import com.caas.contract.domain.ContractId;
import com.caas.contract.domain.ProposalId;
import com.caas.contract.domain.TenantId;
import com.caas.contract.infrastructure.TenantContextHolder;
import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
public class JpaContractRepository implements ContractRepository {

    private final SpringDataContractRepository springDataRepository;
    private final EntityManager entityManager;

    public JpaContractRepository(SpringDataContractRepository springDataRepository, EntityManager entityManager) {
        this.springDataRepository = springDataRepository;
        this.entityManager = entityManager;
    }

    @Override
    @Transactional
    public Contract save(Contract contract) {
        applyTenantContext();
        ContractJpaEntity saved = springDataRepository.save(new ContractJpaEntity(
            contract.id().value(),
            contract.proposalId().value(),
            contract.tenantId().value(),
            contract.funderId(),
            contract.rate(),
            contract.termMonths(),
            contract.amount(),
            contract.signedAt()
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

    private static Contract toDomain(ContractJpaEntity entity) {
        return new Contract(
            new ContractId(entity.getId()),
            new ProposalId(entity.getProposalId()),
            new TenantId(entity.getTenantId()),
            entity.getFunderId(),
            entity.getRate(),
            entity.getTermMonths(),
            entity.getAmount(),
            entity.getSignedAt()
        );
    }
}
