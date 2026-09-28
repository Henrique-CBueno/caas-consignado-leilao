package com.caas.disbursement.infrastructure.web;

import com.caas.disbursement.application.DisbursementRepository;
import com.caas.disbursement.domain.Disbursement;
import com.caas.disbursement.domain.ProposalId;
import java.util.UUID;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
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

    @Operation(summary = "Consulta o desembolso de uma proposta", responses = {
        @ApiResponse(responseCode = "200", description = "Desembolso encontrado"),
        @ApiResponse(responseCode = "404", description = "Ainda não há desembolso para a proposta", content = @io.swagger.v3.oas.annotations.media.Content)
    })
    @Parameter(name = "X-Tenant-Id", in = ParameterIn.HEADER, required = true, description = "Tenant da requisição")
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
