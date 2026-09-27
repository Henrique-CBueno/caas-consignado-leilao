package com.caas.credit.infrastructure.decision;

import static org.assertj.core.api.Assertions.assertThat;

import com.caas.credit.application.CreditDecisionRequest;
import com.caas.credit.domain.CreditDecisionResult;
import com.caas.credit.domain.ProposalId;
import com.caas.credit.domain.TenantId;
import java.math.BigDecimal;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class MockDecisionAdapterTest {

    private final MockDecisionAdapter adapter = new MockDecisionAdapter();

    @Test
    void theSameBorrowerAlwaysProducesTheSameDecision() {
        CreditDecisionRequest request = new CreditDecisionRequest(
            new ProposalId(UUID.randomUUID()),
            new TenantId(UUID.randomUUID()),
            "12345678900",
            new BigDecimal("5000.00")
        );

        CreditDecisionResult first = adapter.decide(request);
        CreditDecisionResult second = adapter.decide(request);

        assertThat(second).isEqualTo(first);
    }
}
