package com.caas.disbursement.infrastructure.kafka;

import com.caas.disbursement.application.ProcessContractSignedUseCase;
import com.caas.events.ContractSignedEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class ContractSignedListener {

    private final ProcessContractSignedUseCase useCase;
    private final ObjectMapper objectMapper;

    public ContractSignedListener(ProcessContractSignedUseCase useCase, ObjectMapper objectMapper) {
        this.useCase = useCase;
        this.objectMapper = objectMapper;
    }

    @KafkaListener(topics = "contract.signed")
    public void onMessage(String payload) throws Exception {
        useCase.execute(objectMapper.readValue(payload, ContractSignedEvent.class));
    }
}
