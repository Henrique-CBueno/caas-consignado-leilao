package com.caas.auction.infrastructure.web;

import com.caas.auction.application.AuctionRepository;
import com.caas.auction.domain.Auction;
import com.caas.auction.domain.AuctionStatus;
import com.caas.auction.domain.ProposalId;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

class InMemoryAuctionRepository implements AuctionRepository {

    private final Map<ProposalId, Auction> auctions = new ConcurrentHashMap<>();

    void clear() {
        auctions.clear();
    }

    @Override
    public Auction save(Auction auction) {
        auctions.put(auction.proposalId(), auction);
        return auction;
    }

    @Override
    public Auction findByProposalId(ProposalId id) {
        return auctions.get(id);
    }

    @Override
    public String traceparentOf(ProposalId id) {
        return null;
    }

    @Override
    public List<Auction> findOpenExpired(Instant now) {
        return auctions.values().stream()
            .filter(a -> a.status() == AuctionStatus.OPEN && now.isAfter(a.expiresAt()))
            .toList();
    }

    @Override
    public void saveAndPublish(Auction auction, UUID aggregateId, String eventType, Object eventPayload) {
        save(auction);
    }
}
