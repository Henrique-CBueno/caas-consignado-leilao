package com.caas.contract.infrastructure.outbox;

import java.time.Instant;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class OutboxRelay {

    private final SpringDataOutboxEventRepository springDataRepository;
    private final KafkaTemplate<String, String> kafkaTemplate;

    public OutboxRelay(SpringDataOutboxEventRepository springDataRepository, KafkaTemplate<String, String> kafkaTemplate) {
        this.springDataRepository = springDataRepository;
        this.kafkaTemplate = kafkaTemplate;
    }

    @Transactional
    @Scheduled(fixedDelayString = "${app.outbox.relay-fixed-delay-ms}")
    public void relayPendingEvents() {
        for (OutboxEventJpaEntity event : springDataRepository.findByPublishedAtIsNullOrderByCreatedAt()) {
            try {
                kafkaTemplate.send(topicFor(event.getEventType()), event.getAggregateId().toString(), event.getPayload()).get();
            } catch (Exception e) {
                throw new IllegalStateException("Falha ao publicar evento de outbox " + event.getId(), e);
            }
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
