package com.caas.events;

import java.math.BigDecimal;
import java.util.UUID;

public record AuctionClosedEvent(
    UUID proposalId,
    UUID tenantId,
    String status,
    String winningFunderId,
    BigDecimal winningRate
) {
}
