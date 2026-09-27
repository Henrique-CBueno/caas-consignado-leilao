package com.caas.proposal.domain

import java.math.BigDecimal
import java.util.UUID

@JvmInline
value class ProposalId(val value: UUID)

@JvmInline
value class TenantId(val value: UUID)

enum class ProposalStatus {
    PENDING_CREDIT_ANALYSIS,
}

data class Proposal(
    val id: ProposalId,
    val tenantId: TenantId,
    val borrowerId: String,
    val requestedAmount: BigDecimal,
    val termMonths: Int,
    val status: ProposalStatus = ProposalStatus.PENDING_CREDIT_ANALYSIS,
)
