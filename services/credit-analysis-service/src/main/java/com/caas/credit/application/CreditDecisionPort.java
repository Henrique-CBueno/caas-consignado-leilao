package com.caas.credit.application;

import com.caas.credit.domain.CreditDecisionResult;

public interface CreditDecisionPort {
    CreditDecisionResult decide(CreditDecisionRequest request);
}
