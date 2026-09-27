package com.caas.credit.application;

import com.caas.credit.domain.CreditDecisionResult;
import com.caas.credit.domain.Decision;
import org.springframework.stereotype.Service;

@Service
public class CreditDecisionService {

    // Nunca automatiza uma decisão de baixa confiança, seja qual for o adapter
    // plugado (mock hoje, Jev real na Milestone 4) — vive aqui, não no adapter.
    private static final double CONFIDENCE_THRESHOLD = 0.6;

    private final CreditDecisionPort port;

    public CreditDecisionService(CreditDecisionPort port) {
        this.port = port;
    }

    public CreditDecisionResult decide(CreditDecisionRequest request) {
        CreditDecisionResult result = port.decide(request);
        if (result.confidence() < CONFIDENCE_THRESHOLD) {
            return new CreditDecisionResult(Decision.MANUAL_REVIEW, result.confidence());
        }
        return result;
    }
}
