package com.caas.auction;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Info;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@OpenAPIDefinition(info = @Info(title = "auction-service", version = "1.0", description = "Leilão reverso de crédito: recebe lances (HTTP) e fecha o leilão pela regra de desempate. Chamado internamente pelo funder-bot-service."))
@SpringBootApplication
@EnableScheduling
public class AuctionServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(AuctionServiceApplication.class, args);
    }
}
