package com.caas.contract.application;

import com.caas.contract.domain.ProposalId;
import com.caas.contract.domain.TenantId;
import com.caas.contract.infrastructure.TenantContextHolder;
import com.caas.events.CreditDecisionMadeEvent;
import org.springframework.stereotype.Service;

@Service
public class ProcessCreditDecisionMadeUseCase {

    private final ContractGenerationService contractGenerationService;

    public ProcessCreditDecisionMadeUseCase(ContractGenerationService contractGenerationService) {
        this.contractGenerationService = contractGenerationService;
    }

    public void execute(CreditDecisionMadeEvent event) {
        if (!"POST_AUCTION".equals(event.stage()) || !"APPROVE".equals(event.decision())) {
            return;
        }

        TenantId tenantId = new TenantId(event.tenantId());
        TenantContextHolder.set(tenantId);
        try {
            contractGenerationService.onCreditApproved(
                new ProposalId(event.proposalId()), tenantId, event.requestedAmount()
            );
        } finally {
            TenantContextHolder.clear();
        }
    }
}
