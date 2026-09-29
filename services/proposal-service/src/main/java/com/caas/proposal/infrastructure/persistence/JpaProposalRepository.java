package com.caas.proposal.infrastructure.persistence;

import com.caas.proposal.application.ProposalPage;
import com.caas.proposal.application.ProposalRepository;
import com.caas.proposal.domain.Proposal;
import com.caas.proposal.domain.ProposalId;
import com.caas.proposal.domain.ProposalStatus;
import com.caas.proposal.domain.TenantId;
import com.caas.proposal.infrastructure.TenantContextHolder;
import jakarta.persistence.EntityManager;
import java.time.Instant;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Slice;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
public class JpaProposalRepository implements ProposalRepository {

    private final SpringDataProposalRepository springDataRepository;
    private final EntityManager entityManager;

    public JpaProposalRepository(SpringDataProposalRepository springDataRepository, EntityManager entityManager) {
        this.springDataRepository = springDataRepository;
        this.entityManager = entityManager;
    }

    @Override
    @Transactional
    public Proposal save(Proposal proposal) {
        applyTenantContext();
        ProposalJpaEntity saved = springDataRepository.save(new ProposalJpaEntity(
            proposal.id().value(),
            proposal.tenantId().value(),
            proposal.borrowerId(),
            proposal.requestedAmount(),
            proposal.termMonths(),
            proposal.status().name(),
            Instant.now()
        ));
        return toDomain(saved);
    }

    // Mesmo isolamento da consulta por id: role restrita + contexto do tenant; sem contexto, a RLS devolve vazio.
    @Override
    @Transactional
    public ProposalPage list(int page, int size) {
        applyTenantContext();
        Slice<ProposalJpaEntity> slice =
            springDataRepository.findAllByOrderByCreatedAtDescIdDesc(PageRequest.of(page, size));
        return new ProposalPage(
            slice.getContent().stream()
                .map(e -> new ProposalPage.Item(
                    e.getId(), e.getBorrowerId(), e.getRequestedAmount(), e.getTermMonths(), e.getCreatedAt()))
                .toList(),
            slice.hasNext());
    }

    @Override
    @Transactional
    public Proposal findById(ProposalId id) {
        applyTenantContext();
        return springDataRepository.findById(id.value()).map(JpaProposalRepository::toDomain).orElse(null);
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

    private static Proposal toDomain(ProposalJpaEntity entity) {
        return new Proposal(
            new ProposalId(entity.getId()),
            new TenantId(entity.getTenantId()),
            entity.getBorrowerId(),
            entity.getRequestedAmount(),
            entity.getTermMonths(),
            ProposalStatus.valueOf(entity.getStatus())
        );
    }
}
