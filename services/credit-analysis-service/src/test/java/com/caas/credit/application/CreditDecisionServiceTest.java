package com.caas.credit.application;

import static org.assertj.core.api.Assertions.assertThat;

import com.caas.credit.domain.CreditDecisionResult;
import com.caas.credit.domain.Decision;
import com.caas.credit.domain.ProposalId;
import com.caas.credit.domain.TenantId;
import java.math.BigDecimal;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class CreditDecisionServiceTest {

    private final CreditDecisionRequest aRequest = new CreditDecisionRequest(
        new ProposalId(UUID.randomUUID()),
        new TenantId(UUID.randomUUID()),
        "12345678900",
        new BigDecimal("5000.00")
    );

    @Test
    void forcesManualReviewWhenConfidenceIsBelowThreshold() {
        CreditDecisionPort port = request -> new CreditDecisionResult(Decision.APPROVE, 0.59);
        CreditDecisionService service = new CreditDecisionService(port);

        CreditDecisionResult result = service.decide(aRequest);

        assertThat(result.decision()).isEqualTo(Decision.MANUAL_REVIEW);
    }

    @Test
    void keepsThePortsDecisionWhenConfidenceMeetsTheThreshold() {
        CreditDecisionPort port = request -> new CreditDecisionResult(Decision.APPROVE, 0.6);
        CreditDecisionService service = new CreditDecisionService(port);

        CreditDecisionResult result = service.decide(aRequest);

        assertThat(result.decision()).isEqualTo(Decision.APPROVE);
        assertThat(result.confidence()).isEqualTo(0.6);
    }

    @Test
    void forcesManualReviewWhenThePortFails() {
        CreditDecisionPort port = request -> {
            throw new IllegalStateException("Jev indisponível");
        };
        CreditDecisionService service = new CreditDecisionService(port);

        CreditDecisionResult result = service.decide(aRequest);

        assertThat(result.decision()).isEqualTo(Decision.MANUAL_REVIEW);
        assertThat(result.confidence()).isEqualTo(0.0);
    }
}
