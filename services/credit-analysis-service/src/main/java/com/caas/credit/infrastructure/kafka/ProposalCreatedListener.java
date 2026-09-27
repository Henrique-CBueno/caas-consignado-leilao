package com.caas.credit.infrastructure.kafka;

import com.caas.credit.application.ProcessProposalCreatedUseCase;
import com.caas.events.ProposalCreatedEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class ProposalCreatedListener {

    private final ProcessProposalCreatedUseCase useCase;
    private final ObjectMapper objectMapper;

    public ProposalCreatedListener(ProcessProposalCreatedUseCase useCase, ObjectMapper objectMapper) {
        this.useCase = useCase;
        this.objectMapper = objectMapper;
    }

    @KafkaListener(topics = "proposal.created")
    public void onMessage(String payload) throws Exception {
        useCase.execute(objectMapper.readValue(payload, ProposalCreatedEvent.class));
    }
}
