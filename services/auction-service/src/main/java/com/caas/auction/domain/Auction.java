package com.caas.auction.domain;

import java.time.Instant;
import java.util.List;

public record Auction(
    ProposalId proposalId,
    TenantId tenantId,
    AuctionStatus status,
    List<String> eligibleFunderIds,
    Instant openedAt,
    Instant expiresAt,
    List<Bid> bids,
    Bid winningBid
) {
}
