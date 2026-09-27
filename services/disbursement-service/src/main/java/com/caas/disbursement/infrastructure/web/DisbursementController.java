package com.caas.disbursement.infrastructure.web;

import com.caas.disbursement.application.DisbursementRepository;
import com.caas.disbursement.domain.Disbursement;
import com.caas.disbursement.domain.ProposalId;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class DisbursementController {

    private final DisbursementRepository disbursementRepository;

    public DisbursementController(DisbursementRepository disbursementRepository) {
        this.disbursementRepository = disbursementRepository;
    }

    @GetMapping("/disbursements/{proposalId}")
    public ResponseEntity<DisbursementResponse> getByProposalId(@PathVariable UUID proposalId) {
        Disbursement disbursement = disbursementRepository.findByProposalId(new ProposalId(proposalId));
        if (disbursement == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(new DisbursementResponse(
            disbursement.proposalId().value(),
            disbursement.funderId(),
            disbursement.amount(),
            disbursement.status(),
            disbursement.disbursedAt()
        ));
    }
}
