package com.caas.auction.domain;

import java.math.BigDecimal;
import java.time.Instant;

public record Bid(String funderId, BigDecimal rate, int termMonths, Instant receivedAt) {
}
