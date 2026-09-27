package com.caas.contract.infrastructure.outbox;

import com.caas.contract.application.OutboxEventPublisher;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class JpaOutboxEventPublisher implements OutboxEventPublisher {

    private final SpringDataOutboxEventRepository springDataRepository;
    private final ObjectMapper objectMapper;

    public JpaOutboxEventPublisher(SpringDataOutboxEventRepository springDataRepository, ObjectMapper objectMapper) {
        this.springDataRepository = springDataRepository;
        this.objectMapper = objectMapper;
    }

    @Override
    public void publish(String aggregateType, UUID aggregateId, String eventType, Object payload) {
        try {
            springDataRepository.save(new OutboxEventJpaEntity(
                UUID.randomUUID(),
                aggregateType,
                aggregateId,
                eventType,
                objectMapper.writeValueAsString(payload),
                Instant.now(),
                null
            ));
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Falha ao serializar payload do evento " + eventType, e);
        }
    }
}
