package com.caas.events;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record AuctionOpenedEvent(
    UUID proposalId,
    UUID tenantId,
    List<String> eligibleFunderIds,
    Instant openedAt,
    Instant expiresAt
) {
}
