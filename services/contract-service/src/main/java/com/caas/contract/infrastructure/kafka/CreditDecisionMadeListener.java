package com.caas.contract.infrastructure.kafka;

import com.caas.contract.application.ProcessCreditDecisionMadeUseCase;
import com.caas.events.CreditDecisionMadeEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class CreditDecisionMadeListener {

    private final ProcessCreditDecisionMadeUseCase useCase;
    private final ObjectMapper objectMapper;

    public CreditDecisionMadeListener(ProcessCreditDecisionMadeUseCase useCase, ObjectMapper objectMapper) {
        this.useCase = useCase;
        this.objectMapper = objectMapper;
    }

    @KafkaListener(topics = "credit.decision.made")
    public void onMessage(String payload) throws Exception {
        useCase.execute(objectMapper.readValue(payload, CreditDecisionMadeEvent.class));
    }
}
