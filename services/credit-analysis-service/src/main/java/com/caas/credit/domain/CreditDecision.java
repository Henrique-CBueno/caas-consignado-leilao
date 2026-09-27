package com.caas.credit.domain;

public record CreditDecision(
    CreditDecisionId id,
    ProposalId proposalId,
    TenantId tenantId,
    Decision decision,
    double confidence
) {
}
