package com.caas.proposal.infrastructure.web;

import java.math.BigDecimal;

public record CreateProposalRequest(String borrowerId, BigDecimal requestedAmount, int termMonths) {
}
