package com.caas.proposal.application;

import com.caas.proposal.domain.TenantId;
import java.math.BigDecimal;

public record CreateProposalCommand(
    TenantId tenantId,
    String borrowerId,
    BigDecimal requestedAmount,
    int termMonths
) {
}
