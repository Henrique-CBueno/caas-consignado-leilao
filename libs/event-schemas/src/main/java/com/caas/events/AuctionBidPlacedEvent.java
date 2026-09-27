package com.caas.events;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record AuctionBidPlacedEvent(
    UUID proposalId,
    UUID tenantId,
    String funderId,
    BigDecimal rate,
    int termMonths,
    Instant receivedAt
) {
}
