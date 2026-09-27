package com.caas.proposal.application;

import com.caas.proposal.domain.Proposal;
import com.caas.proposal.domain.ProposalId;

public interface ProposalRepository {
    Proposal save(Proposal proposal);

    Proposal findById(ProposalId id);
}
