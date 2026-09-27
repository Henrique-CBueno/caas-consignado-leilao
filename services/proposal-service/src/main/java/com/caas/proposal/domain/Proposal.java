package com.caas.proposal.domain;

import java.math.BigDecimal;

public record Proposal(
    ProposalId id,
    TenantId tenantId,
    String borrowerId,
    BigDecimal requestedAmount,
    int termMonths,
    ProposalStatus status
) {

    public Proposal(ProposalId id, TenantId tenantId, String borrowerId, BigDecimal requestedAmount, int termMonths) {
        this(id, tenantId, borrowerId, requestedAmount, termMonths, ProposalStatus.PENDING_CREDIT_ANALYSIS);
    }
}
