package com.caas.funderbot.infrastructure.client;

import java.math.BigDecimal;

public record BidPayload(String funderId, BigDecimal rate, int termMonths) {
}
