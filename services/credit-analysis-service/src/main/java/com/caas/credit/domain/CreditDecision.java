package com.caas.credit.domain;

import java.math.BigDecimal;

public record CreditDecision(
    CreditDecisionId id,
    ProposalId proposalId,
    TenantId tenantId,
    String borrowerId,
    BigDecimal requestedAmount,
    Decision decision,
    double confidence,
    Stage stage
) {
}
