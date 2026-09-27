package com.caas.credit.application;

import com.caas.credit.domain.ProposalId;
import com.caas.credit.domain.TenantId;
import java.math.BigDecimal;

public record CreditDecisionRequest(
    ProposalId proposalId,
    TenantId tenantId,
    String borrowerId,
    BigDecimal requestedAmount
) {
}
