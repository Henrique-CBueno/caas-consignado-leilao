package com.caas.credit.application;

import com.caas.credit.domain.CreditDecision;
import com.caas.credit.domain.CreditDecisionId;
import com.caas.credit.domain.CreditDecisionResult;
import com.caas.credit.domain.ProposalId;
import com.caas.credit.domain.TenantId;
import com.caas.credit.infrastructure.TenantContextHolder;
import com.caas.events.CreditDecisionMadeEvent;
import com.caas.events.ProposalCreatedEvent;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ProcessProposalCreatedUseCase {

    private final CreditDecisionService creditDecisionService;
    private final CreditDecisionRepository creditDecisionRepository;
    private final OutboxEventPublisher outboxEventPublisher;

    public ProcessProposalCreatedUseCase(
        CreditDecisionService creditDecisionService,
        CreditDecisionRepository creditDecisionRepository,
        OutboxEventPublisher outboxEventPublisher
    ) {
        this.creditDecisionService = creditDecisionService;
        this.creditDecisionRepository = creditDecisionRepository;
        this.outboxEventPublisher = outboxEventPublisher;
    }

    // @Transactional junto com o outbox garante que a decisão persistida e o
    // evento pendente andam atômicos, igual ao CreateProposalUseCase (ADR-0003).
    @Transactional
    public void execute(ProposalCreatedEvent event) {
        TenantId tenantId = new TenantId(event.tenantId());
        TenantContextHolder.set(tenantId);
        try {
            CreditDecisionRequest request = new CreditDecisionRequest(
                new ProposalId(event.proposalId()),
                tenantId,
                event.borrowerId(),
                event.requestedAmount()
            );
            CreditDecisionResult result = creditDecisionService.decide(request);
            CreditDecision decision = new CreditDecision(
                new CreditDecisionId(UUID.randomUUID()),
                request.proposalId(),
                tenantId,
                result.decision(),
                result.confidence()
            );
            creditDecisionRepository.save(decision);

            outboxEventPublisher.publish(
                "CreditDecision",
                decision.proposalId().value(),
                "CreditDecisionMade",
                new CreditDecisionMadeEvent(
                    decision.proposalId().value(),
                    decision.tenantId().value(),
                    decision.decision().name(),
                    decision.confidence()
                )
            );
        } finally {
            TenantContextHolder.clear();
        }
    }
}
