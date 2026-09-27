package com.caas.contract.application;

import com.caas.contract.domain.ProposalId;
import com.caas.contract.domain.TenantId;
import com.caas.contract.infrastructure.TenantContextHolder;
import com.caas.events.AuctionClosedEvent;
import org.springframework.stereotype.Service;

@Service
public class ProcessAuctionClosedUseCase {

    private static final String CLOSED_WITH_WINNER = "CLOSED_WITH_WINNER";

    private final ContractGenerationService contractGenerationService;

    public ProcessAuctionClosedUseCase(ContractGenerationService contractGenerationService) {
        this.contractGenerationService = contractGenerationService;
    }

    public void execute(AuctionClosedEvent event) {
        if (!CLOSED_WITH_WINNER.equals(event.status())) {
            return;
        }

        TenantId tenantId = new TenantId(event.tenantId());
        TenantContextHolder.set(tenantId);
        try {
            contractGenerationService.onAuctionWon(
                new ProposalId(event.proposalId()), tenantId,
                event.winningFunderId(), event.winningRate(), event.winningTermMonths()
            );
        } finally {
            TenantContextHolder.clear();
        }
    }
}
