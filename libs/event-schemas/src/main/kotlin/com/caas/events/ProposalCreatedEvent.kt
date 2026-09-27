package com.caas.events

import java.math.BigDecimal
import java.util.UUID

data class ProposalCreatedEvent(
    val proposalId: UUID,
    val tenantId: UUID,
    val borrowerId: String,
    val requestedAmount: BigDecimal,
    val termMonths: Int,
)
