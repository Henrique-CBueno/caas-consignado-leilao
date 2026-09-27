package com.caas.contract.domain;

import java.math.BigDecimal;

// Agregado que espera os dois fatos assíncronos (revalidação de crédito aprovada
// e leilão fechado com vencedor) chegarem em qualquer ordem, por proposalId.
// Ver ADR do mecanismo de correlação (Milestone 8).
public record ContractCorrelation(
    ProposalId proposalId,
    TenantId tenantId,
    BigDecimal requestedAmount,
    String winningFunderId,
    BigDecimal winningRate,
    Integer winningTermMonths,
    boolean contractGenerated
) {

    public static ContractCorrelation empty(ProposalId proposalId, TenantId tenantId) {
        return new ContractCorrelation(proposalId, tenantId, null, null, null, null, false);
    }

    public ContractCorrelation withCreditApproved(BigDecimal requestedAmount) {
        return new ContractCorrelation(
            proposalId, tenantId, requestedAmount, winningFunderId, winningRate, winningTermMonths, contractGenerated
        );
    }

    public ContractCorrelation withAuctionWinner(String funderId, BigDecimal rate, Integer termMonths) {
        return new ContractCorrelation(
            proposalId, tenantId, requestedAmount, funderId, rate, termMonths, contractGenerated
        );
    }

    public ContractCorrelation markContractGenerated() {
        return new ContractCorrelation(
            proposalId, tenantId, requestedAmount, winningFunderId, winningRate, winningTermMonths, true
        );
    }

    public boolean isComplete() {
        return requestedAmount != null && winningFunderId != null;
    }
}
