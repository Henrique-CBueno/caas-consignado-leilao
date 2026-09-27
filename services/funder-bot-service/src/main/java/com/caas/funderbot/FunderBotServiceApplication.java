package com.caas.funderbot;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class FunderBotServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(FunderBotServiceApplication.class, args);
    }
}
