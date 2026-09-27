package com.caas.proposal;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class ProposalServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(ProposalServiceApplication.class, args);
    }
}
