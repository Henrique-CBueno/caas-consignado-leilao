package com.caas.proposal.application;

import com.caas.events.ProposalCreatedEvent;
import com.caas.proposal.domain.Proposal;
import com.caas.proposal.domain.ProposalId;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CreateProposalUseCase {

    private final ProposalRepository proposalRepository;
    private final OutboxEventPublisher outboxEventPublisher;

    public CreateProposalUseCase(ProposalRepository proposalRepository, OutboxEventPublisher outboxEventPublisher) {
        this.proposalRepository = proposalRepository;
        this.outboxEventPublisher = outboxEventPublisher;
    }

    // @Transactional aqui é o que faz a gravação da proposta e do evento de outbox
    // andarem na mesma transação: os dois adapters usam propagação REQUIRED (padrão),
    // então entram na transação já aberta por este método em vez de abrir a própria.
    @Transactional
    public Proposal execute(CreateProposalCommand command) {
        Proposal proposal = new Proposal(
            new ProposalId(UUID.randomUUID()),
            command.tenantId(),
            command.borrowerId(),
            command.requestedAmount(),
            command.termMonths()
        );
        Proposal saved = proposalRepository.save(proposal);

        outboxEventPublisher.publish(
            "Proposal",
            saved.id().value(),
            "ProposalCreated",
            new ProposalCreatedEvent(
                saved.id().value(),
                saved.tenantId().value(),
                saved.borrowerId(),
                saved.requestedAmount(),
                saved.termMonths()
            )
        );

        return saved;
    }
}
