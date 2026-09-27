package com.caas.contract.infrastructure.kafka;

import com.caas.contract.application.ProcessAuctionClosedUseCase;
import com.caas.events.AuctionClosedEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class AuctionClosedListener {

    private final ProcessAuctionClosedUseCase useCase;
    private final ObjectMapper objectMapper;

    public AuctionClosedListener(ProcessAuctionClosedUseCase useCase, ObjectMapper objectMapper) {
        this.useCase = useCase;
        this.objectMapper = objectMapper;
    }

    @KafkaListener(topics = "auction.closed")
    public void onMessage(String payload) throws Exception {
        useCase.execute(objectMapper.readValue(payload, AuctionClosedEvent.class));
    }
}
