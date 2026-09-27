package com.caas.contract.infrastructure.persistence;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SpringDataContractCorrelationRepository extends JpaRepository<ContractCorrelationJpaEntity, UUID> {
    ContractCorrelationJpaEntity findByProposalId(UUID proposalId);
}
