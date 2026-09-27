package com.caas.credit.infrastructure.persistence;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SpringDataCreditDecisionRepository extends JpaRepository<CreditDecisionJpaEntity, UUID> {
}
