package com.caas.disbursement;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Info;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@OpenAPIDefinition(info = @Info(title = "disbursement-service", version = "1.0", description = "Consulta de desembolsos simulados, registrados a partir do evento ContractSigned."))
@SpringBootApplication
public class DisbursementServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(DisbursementServiceApplication.class, args);
    }
}
