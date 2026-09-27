package com.caas.proposal

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.scheduling.annotation.EnableScheduling

@SpringBootApplication
@EnableScheduling
class ProposalServiceApplication

fun main(args: Array<String>) {
    runApplication<ProposalServiceApplication>(*args)
}
