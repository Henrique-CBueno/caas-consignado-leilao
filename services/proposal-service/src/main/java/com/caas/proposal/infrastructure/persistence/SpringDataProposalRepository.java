package com.caas.proposal.infrastructure.persistence;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SpringDataProposalRepository extends JpaRepository<ProposalJpaEntity, UUID> {
}
