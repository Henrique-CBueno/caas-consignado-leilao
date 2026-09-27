package com.caas.disbursement.infrastructure.persistence;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SpringDataDisbursementRepository extends JpaRepository<DisbursementJpaEntity, UUID> {
}
