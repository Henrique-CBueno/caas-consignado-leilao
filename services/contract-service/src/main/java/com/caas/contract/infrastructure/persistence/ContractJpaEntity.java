package com.caas.contract.infrastructure.persistence;

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
@Table(name = "contracts")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class ContractJpaEntity {

    @Id
    private UUID id;

    private UUID proposalId;
    private UUID tenantId;
    private String funderId;
    private BigDecimal rate;
    private int termMonths;
    private BigDecimal amount;
    private Instant signedAt;
}
