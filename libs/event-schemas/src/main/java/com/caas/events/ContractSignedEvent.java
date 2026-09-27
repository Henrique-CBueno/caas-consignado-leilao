package com.caas.events;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record ContractSignedEvent(
    UUID proposalId,
    UUID tenantId,
    String funderId,
    BigDecimal rate,
    int termMonths,
    BigDecimal amount,
    Instant signedAt
) {
}
