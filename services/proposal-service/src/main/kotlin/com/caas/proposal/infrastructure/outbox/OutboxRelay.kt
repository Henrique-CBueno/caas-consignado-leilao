package com.caas.proposal.infrastructure.outbox

import org.springframework.kafka.core.KafkaTemplate
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.time.Instant

@Component
class OutboxRelay(
    private val springDataRepository: SpringDataOutboxEventRepository,
    private val kafkaTemplate: KafkaTemplate<String, String>,
) {
    // Sem RLS na outbox (ver migration V4): varre eventos pendentes de todos os
    // tenants, então nenhum tenant context é setado aqui.
    // Entidades vêm gerenciadas pelo EntityManager desta transação: mutar
    // publishedAt basta, o dirty checking do Hibernate grava a mudança.
    @Transactional
    @Scheduled(fixedDelayString = "\${app.outbox.relay-fixed-delay-ms}")
    fun relayPendingEvents() {
        springDataRepository.findByPublishedAtIsNullOrderByCreatedAt().forEach { event ->
            kafkaTemplate.send(topicFor(event.eventType), event.aggregateId.toString(), event.payload).get()
            event.publishedAt = Instant.now()
        }
    }

    // Único mapeamento existente por enquanto — vira um registro de verdade
    // se/quando um segundo tipo de evento aparecer (YAGNI).
    private fun topicFor(eventType: String) = when (eventType) {
        "ProposalCreated" -> "proposal.created"
        else -> error("Sem tópico Kafka mapeado para o evento '$eventType'")
    }
}
