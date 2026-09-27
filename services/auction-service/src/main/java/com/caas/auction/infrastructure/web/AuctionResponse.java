package com.caas.auction.infrastructure.web;

import com.caas.auction.domain.Bid;
import java.util.List;
import java.util.UUID;

public record AuctionResponse(UUID proposalId, String status, List<Bid> bids, String winningFunderId) {
}
