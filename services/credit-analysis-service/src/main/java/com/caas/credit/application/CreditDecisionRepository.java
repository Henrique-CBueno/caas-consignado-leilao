package com.caas.credit.application;

import com.caas.credit.domain.CreditDecision;
import com.caas.credit.domain.CreditDecisionId;

public interface CreditDecisionRepository {
    CreditDecision save(CreditDecision creditDecision);

    CreditDecision findById(CreditDecisionId id);
}
