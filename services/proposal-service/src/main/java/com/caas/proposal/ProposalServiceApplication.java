package com.caas.proposal;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Info;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@OpenAPIDefinition(info = @Info(title = "proposal-service", version = "1.0", description = "Originação de propostas de crédito consignado. Publica ProposalCreated no Kafka via outbox transacional."))
@SpringBootApplication
@EnableScheduling
public class ProposalServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(ProposalServiceApplication.class, args);
    }
}
