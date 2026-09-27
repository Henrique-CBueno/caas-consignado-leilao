package com.caas.events;

import java.math.BigDecimal;
import java.util.UUID;

public record ProposalCreatedEvent(
    UUID proposalId,
    UUID tenantId,
    String borrowerId,
    BigDecimal requestedAmount,
    int termMonths
) {
}
