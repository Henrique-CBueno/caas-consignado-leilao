package com.caas.credit.application;

import com.caas.credit.domain.CreditDecision;
import com.caas.credit.domain.CreditDecisionId;
import com.caas.credit.domain.CreditDecisionResult;
import com.caas.credit.domain.ProposalId;
import com.caas.credit.domain.Stage;
import com.caas.credit.domain.TenantId;
import com.caas.credit.infrastructure.TenantContextHolder;
import com.caas.events.AuctionClosedEvent;
import com.caas.events.CreditDecisionMadeEvent;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ProcessAuctionClosedUseCase {

    private static final String CLOSED_WITH_WINNER = "CLOSED_WITH_WINNER";

    private final CreditDecisionService creditDecisionService;
    private final CreditDecisionRepository creditDecisionRepository;
    private final OutboxEventPublisher outboxEventPublisher;

    public ProcessAuctionClosedUseCase(
        CreditDecisionService creditDecisionService,
        CreditDecisionRepository creditDecisionRepository,
        OutboxEventPublisher outboxEventPublisher
    ) {
        this.creditDecisionService = creditDecisionService;
        this.creditDecisionRepository = creditDecisionRepository;
        this.outboxEventPublisher = outboxEventPublisher;
    }

    @Transactional
    public void execute(AuctionClosedEvent event) {
        if (!CLOSED_WITH_WINNER.equals(event.status())) {
            return;
        }

        TenantId tenantId = new TenantId(event.tenantId());
        TenantContextHolder.set(tenantId);
        try {
            ProposalId proposalId = new ProposalId(event.proposalId());
            CreditDecision preAuction =
                creditDecisionRepository.findByProposalIdAndStage(proposalId, Stage.PRE_AUCTION);
            if (preAuction == null) {
                return;
            }

            CreditDecisionRequest request = new CreditDecisionRequest(
                proposalId, tenantId, preAuction.borrowerId(), preAuction.requestedAmount()
            );
            CreditDecisionResult result = creditDecisionService.decide(request);
            CreditDecision postAuction = new CreditDecision(
                new CreditDecisionId(UUID.randomUUID()),
                proposalId,
                tenantId,
                preAuction.borrowerId(),
                preAuction.requestedAmount(),
                result.decision(),
                result.confidence(),
                Stage.POST_AUCTION
            );
            creditDecisionRepository.save(postAuction);

            outboxEventPublisher.publish(
                "CreditDecision",
                postAuction.proposalId().value(),
                "CreditDecisionMade",
                new CreditDecisionMadeEvent(
                    postAuction.proposalId().value(),
                    postAuction.tenantId().value(),
                    postAuction.decision().name(),
                    postAuction.confidence(),
                    postAuction.stage().name(),
                    postAuction.requestedAmount()
                )
            );
        } finally {
            TenantContextHolder.clear();
        }
    }
}
