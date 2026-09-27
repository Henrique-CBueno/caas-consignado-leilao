package com.caas.disbursement.infrastructure.web;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record DisbursementResponse(
    UUID proposalId,
    String funderId,
    BigDecimal amount,
    String status,
    Instant disbursedAt
) {
}
