package com.caas.proposal.infrastructure.persistence;

import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SpringDataProposalRepository extends JpaRepository<ProposalJpaEntity, UUID> {

    // Ordem determinística (data e depois id) para a paginação não repetir nem pular linhas.
    Slice<ProposalJpaEntity> findAllByOrderByCreatedAtDescIdDesc(Pageable pageable);
}
