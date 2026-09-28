package com.caas.contract.infrastructure.outbox;

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
        this.traceContextStore = traceContextStore;
        this.springDataRepository = springDataRepository;
        this.kafkaTemplate = kafkaTemplate;
    }

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

    private String topicFor(String eventType) {
        if ("ContractSigned".equals(eventType)) {
            return "contract.signed";
        }
        throw new IllegalStateException("Sem tópico Kafka mapeado para o evento '" + eventType + "'");
    }
}
