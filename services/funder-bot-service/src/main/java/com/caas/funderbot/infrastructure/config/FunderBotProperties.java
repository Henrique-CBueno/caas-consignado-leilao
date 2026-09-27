package com.caas.funderbot.infrastructure.config;

import java.math.BigDecimal;
import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.funder-bot")
public record FunderBotProperties(long minDelayMs, long maxDelayMs, List<Bot> bots) {

    public record Bot(String id, BigDecimal baseRate, int termMonths) {
    }
}
