package com.caas.disbursement.domain;

import java.math.BigDecimal;
import java.time.Instant;

public record Disbursement(
    ProposalId proposalId,
    TenantId tenantId,
    String funderId,
    BigDecimal amount,
    String status,
    Instant disbursedAt
) {
}
