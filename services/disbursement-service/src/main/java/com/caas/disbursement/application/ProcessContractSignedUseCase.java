package com.caas.disbursement.application;

import com.caas.disbursement.domain.Disbursement;
import com.caas.disbursement.domain.ProposalId;
import com.caas.disbursement.domain.TenantId;
import com.caas.disbursement.infrastructure.TenantContextHolder;
import com.caas.events.ContractSignedEvent;
import java.time.Instant;
import org.springframework.stereotype.Service;

@Service
public class ProcessContractSignedUseCase {

    private static final String DISBURSED = "DISBURSED";

    private final DisbursementRepository disbursementRepository;

    public ProcessContractSignedUseCase(DisbursementRepository disbursementRepository) {
        this.disbursementRepository = disbursementRepository;
    }

    public void execute(ContractSignedEvent event) {
        TenantId tenantId = new TenantId(event.tenantId());
        TenantContextHolder.set(tenantId);
        try {
            Disbursement disbursement = new Disbursement(
                new ProposalId(event.proposalId()),
                tenantId,
                event.funderId(),
                event.amount(),
                DISBURSED,
                Instant.now()
            );
            disbursementRepository.save(disbursement);
        } finally {
            TenantContextHolder.clear();
        }
    }
}
