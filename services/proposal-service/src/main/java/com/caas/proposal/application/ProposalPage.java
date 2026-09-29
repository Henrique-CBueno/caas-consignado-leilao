package com.caas.proposal.application;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

// Uma página das propostas do tenant (da mais recente para a mais antiga). Sem status: hoje ele nunca
// avança de PENDING_CREDIT_ANALYSIS, então exibi-lo enganaria (ADR-0031).
public record ProposalPage(List<Item> items, boolean hasNext) {

    public record Item(UUID id, String borrowerId, BigDecimal requestedAmount, int termMonths, Instant createdAt) {
    }
}
