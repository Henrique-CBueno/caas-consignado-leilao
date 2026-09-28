package com.caas.proposal.infrastructure.outbox;

import com.caas.observability.TraceContextStore;
import com.caas.proposal.application.OutboxEventPublisher;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class JpaOutboxEventPublisher implements OutboxEventPublisher {

    private final SpringDataOutboxEventRepository springDataRepository;
    private final ObjectMapper objectMapper;
    private final TraceContextStore traceContextStore;

    public JpaOutboxEventPublisher(
        SpringDataOutboxEventRepository springDataRepository,
        ObjectMapper objectMapper,
        TraceContextStore traceContextStore
    ) {
        this.springDataRepository = springDataRepository;
        this.objectMapper = objectMapper;
        this.traceContextStore = traceContextStore;
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
                null,
                traceContextStore.capture()
            ));
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Falha ao serializar payload do evento " + eventType, e);
        }
    }
}
