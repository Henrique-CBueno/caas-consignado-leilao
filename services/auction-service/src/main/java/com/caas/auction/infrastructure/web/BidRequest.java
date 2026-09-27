package com.caas.auction.infrastructure.web;

import java.math.BigDecimal;

public record BidRequest(String funderId, BigDecimal rate, int termMonths) {
}
