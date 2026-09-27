package com.caas.proposal.infrastructure.web;

import java.math.BigDecimal;
import java.util.UUID;

public record CreateProposalResponse(
    UUID id,
    String borrowerId,
    BigDecimal requestedAmount,
    int termMonths,
    String status
) {
}
