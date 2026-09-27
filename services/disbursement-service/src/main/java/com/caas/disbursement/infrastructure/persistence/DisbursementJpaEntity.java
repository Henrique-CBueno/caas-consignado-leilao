package com.caas.disbursement.infrastructure.persistence;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "disbursements")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class DisbursementJpaEntity {

    @Id
    private UUID proposalId;

    private UUID tenantId;
    private String funderId;
    private BigDecimal amount;
    private String status;
    private Instant disbursedAt;
}
