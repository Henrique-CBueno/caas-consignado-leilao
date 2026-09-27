package com.caas.contract.infrastructure.persistence;

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
@Table(name = "contract_correlations")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class ContractCorrelationJpaEntity {

    @Id
    private UUID proposalId;

    private UUID tenantId;
    private BigDecimal requestedAmount;
    private String winningFunderId;
    private BigDecimal winningRate;
    private Integer winningTermMonths;
    private boolean contractGenerated;
}
