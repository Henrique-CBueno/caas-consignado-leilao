package com.caas.proposal.infrastructure.web;

import com.caas.proposal.application.CreateProposalCommand;
import com.caas.proposal.application.CreateProposalUseCase;
import com.caas.proposal.application.ProposalRepository;
import com.caas.proposal.domain.Proposal;
import com.caas.proposal.domain.ProposalId;
import com.caas.proposal.domain.TenantId;
import com.caas.proposal.infrastructure.TenantContextHolder;
import java.math.BigDecimal;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
public class ProposalController {

    private final ProposalRepository proposalRepository;
    private final CreateProposalUseCase createProposalUseCase;

    public ProposalController(ProposalRepository proposalRepository, CreateProposalUseCase createProposalUseCase) {
        this.proposalRepository = proposalRepository;
        this.createProposalUseCase = createProposalUseCase;
    }

    @PostMapping("/proposals")
    @ResponseStatus(HttpStatus.CREATED)
    public CreateProposalResponse create(@RequestBody CreateProposalRequest request) {
        if (request.requestedAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "requestedAmount deve ser maior que zero");
        }
        if (request.termMonths() <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "termMonths deve ser maior que zero");
        }
        TenantId tenantId = TenantContextHolder.get();
        if (tenantId == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        }

        Proposal saved = createProposalUseCase.execute(new CreateProposalCommand(
            tenantId,
            request.borrowerId(),
            request.requestedAmount(),
            request.termMonths()
        ));
        return toResponse(saved);
    }

    @GetMapping("/proposals/{id}")
    public ResponseEntity<CreateProposalResponse> getById(@PathVariable UUID id) {
        Proposal proposal = proposalRepository.findById(new ProposalId(id));
        if (proposal == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(toResponse(proposal));
    }

    private static CreateProposalResponse toResponse(Proposal proposal) {
        return new CreateProposalResponse(
            proposal.id().value(),
            proposal.borrowerId(),
            proposal.requestedAmount(),
            proposal.termMonths(),
            proposal.status().name()
        );
    }
}
