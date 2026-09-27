package com.caas.credit.application;

import java.util.UUID;

public interface OutboxEventPublisher {
    void publish(String aggregateType, UUID aggregateId, String eventType, Object payload);
}
