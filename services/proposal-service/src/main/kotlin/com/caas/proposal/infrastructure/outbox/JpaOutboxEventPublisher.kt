package com.caas.proposal.infrastructure.outbox

import com.caas.proposal.application.OutboxEventPublisher
import com.fasterxml.jackson.databind.ObjectMapper
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Component
import java.time.Instant
import java.util.UUID

@Entity
@Table(name = "outbox_events")
class OutboxEventJpaEntity(
    @Id
    val id: UUID,
    val aggregateType: String,
    val aggregateId: UUID,
    val eventType: String,
    @Column(columnDefinition = "TEXT")
    val payload: String,
    val createdAt: Instant,
    var publishedAt: Instant? = null,
) {
    protected constructor() : this(UUID(0, 0), "", UUID(0, 0), "", "", Instant.EPOCH, null)
}

interface SpringDataOutboxEventRepository : JpaRepository<OutboxEventJpaEntity, UUID> {
    fun findByPublishedAtIsNullOrderByCreatedAt(): List<OutboxEventJpaEntity>
}

@Component
class JpaOutboxEventPublisher(
    private val springDataRepository: SpringDataOutboxEventRepository,
    private val objectMapper: ObjectMapper,
) : OutboxEventPublisher {
    override fun publish(
        aggregateType: String,
        aggregateId: UUID,
        eventType: String,
        payload: Any,
    ) {
        springDataRepository.save(
            OutboxEventJpaEntity(
                id = UUID.randomUUID(),
                aggregateType = aggregateType,
                aggregateId = aggregateId,
                eventType = eventType,
                payload = objectMapper.writeValueAsString(payload),
                createdAt = Instant.now(),
            ),
        )
    }
}
