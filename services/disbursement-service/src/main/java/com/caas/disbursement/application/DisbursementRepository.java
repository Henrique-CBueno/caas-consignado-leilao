package com.caas.disbursement.application;

import com.caas.disbursement.domain.Disbursement;
import com.caas.disbursement.domain.ProposalId;

public interface DisbursementRepository {
    Disbursement save(Disbursement disbursement);

    Disbursement findByProposalId(ProposalId proposalId);
}
