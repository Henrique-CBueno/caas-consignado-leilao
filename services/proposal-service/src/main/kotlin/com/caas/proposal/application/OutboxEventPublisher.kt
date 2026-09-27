package com.caas.proposal.application

import java.util.UUID

interface OutboxEventPublisher {
    fun publish(
        aggregateType: String,
        aggregateId: UUID,
        eventType: String,
        payload: Any,
    )
}
