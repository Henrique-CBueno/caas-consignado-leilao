package com.caas.contract.domain;

import java.math.BigDecimal;
import java.time.Instant;

public record Contract(
    ContractId id,
    ProposalId proposalId,
    TenantId tenantId,
    String funderId,
    BigDecimal rate,
    int termMonths,
    BigDecimal amount,
    Instant signedAt
) {
}
