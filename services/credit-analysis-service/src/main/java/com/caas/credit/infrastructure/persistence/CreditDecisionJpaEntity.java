package com.caas.credit.infrastructure.persistence;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "credit_decisions")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class CreditDecisionJpaEntity {

    @Id
    private UUID id;

    private UUID proposalId;
    private UUID tenantId;
    private String borrowerId;
    private BigDecimal requestedAmount;
    private String decision;
    private double confidence;
    private String stage;
}
