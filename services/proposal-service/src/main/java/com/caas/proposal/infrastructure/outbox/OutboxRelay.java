package com.caas.proposal.infrastructure.outbox;

import com.caas.observability.TraceContextStore;
import java.time.Instant;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class OutboxRelay {

    private final SpringDataOutboxEventRepository springDataRepository;
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final TraceContextStore traceContextStore;

    public OutboxRelay(
        SpringDataOutboxEventRepository springDataRepository,
        KafkaTemplate<String, String> kafkaTemplate,
        TraceContextStore traceContextStore
    ) {
        this.springDataRepository = springDataRepository;
        this.kafkaTemplate = kafkaTemplate;
        this.traceContextStore = traceContextStore;
    }

    // Sem RLS na outbox (ver migration V4): varre eventos pendentes de todos os
    // tenants, então nenhum tenant context é setado aqui.
    // Entidades vêm gerenciadas pelo EntityManager desta transação: mutar
    // publishedAt basta, o dirty checking do Hibernate grava a mudança.
    @Transactional
    @Scheduled(fixedDelayString = "${app.outbox.relay-fixed-delay-ms}")
    public void relayPendingEvents() {
        for (OutboxEventJpaEntity event : springDataRepository.findByPublishedAtIsNullOrderByCreatedAt()) {
            traceContextStore.inSpan(event.getTraceparent(), "outbox-relay " + event.getEventType(), () -> {
                try {
                    kafkaTemplate.send(topicFor(event.getEventType()), event.getAggregateId().toString(), event.getPayload()).get();
                } catch (Exception e) {
                    throw new IllegalStateException("Falha ao publicar evento de outbox " + event.getId(), e);
                }
            });
            event.setPublishedAt(Instant.now());
        }
    }

    // Único mapeamento existente por enquanto — vira um registro de verdade
    // se/quando um segundo tipo de evento aparecer (YAGNI).
    private String topicFor(String eventType) {
        if ("ProposalCreated".equals(eventType)) {
            return "proposal.created";
        }
        throw new IllegalStateException("Sem tópico Kafka mapeado para o evento '" + eventType + "'");
    }
}
