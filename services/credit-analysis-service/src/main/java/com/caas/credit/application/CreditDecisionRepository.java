package com.caas.credit.application;

import com.caas.credit.domain.CreditDecision;
import com.caas.credit.domain.CreditDecisionId;
import com.caas.credit.domain.ProposalId;
import com.caas.credit.domain.Stage;

public interface CreditDecisionRepository {
    CreditDecision save(CreditDecision creditDecision);

    CreditDecision findById(CreditDecisionId id);

    CreditDecision findByProposalIdAndStage(ProposalId proposalId, Stage stage);
}
