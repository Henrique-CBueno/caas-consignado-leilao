package com.caas.contract.infrastructure.persistence;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SpringDataContractRepository extends JpaRepository<ContractJpaEntity, UUID> {
}
