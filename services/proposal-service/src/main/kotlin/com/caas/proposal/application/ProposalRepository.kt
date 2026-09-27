package com.caas.proposal.application

import com.caas.proposal.domain.Proposal
import com.caas.proposal.domain.ProposalId

interface ProposalRepository {
    fun save(proposal: Proposal): Proposal

    fun findById(id: ProposalId): Proposal?
}
