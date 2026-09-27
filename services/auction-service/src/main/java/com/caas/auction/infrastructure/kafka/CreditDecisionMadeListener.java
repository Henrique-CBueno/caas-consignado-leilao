package com.caas.auction.infrastructure.kafka;

import com.caas.auction.application.AuctionRepository;
import com.caas.auction.domain.Auction;
import com.caas.auction.domain.AuctionStatus;
import com.caas.auction.domain.ProposalId;
import com.caas.auction.domain.TenantId;
import com.caas.events.AuctionOpenedEvent;
import com.caas.events.CreditDecisionMadeEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class CreditDecisionMadeListener {

    private final AuctionRepository auctionRepository;
    private final ObjectMapper objectMapper;
    private final long windowSeconds;
    private final List<String> eligibleFunderIds;

    public CreditDecisionMadeListener(
        AuctionRepository auctionRepository,
        ObjectMapper objectMapper,
        @Value("${app.auction.window-seconds}") long windowSeconds,
        @Value("${app.auction.eligible-funder-ids}") String eligibleFunderIdsCsv
    ) {
        this.auctionRepository = auctionRepository;
        this.objectMapper = objectMapper;
        this.windowSeconds = windowSeconds;
        this.eligibleFunderIds = Arrays.asList(eligibleFunderIdsCsv.split(","));
    }

    @KafkaListener(topics = "credit.decision.made")
    public void onMessage(String payload) throws Exception {
        CreditDecisionMadeEvent event = objectMapper.readValue(payload, CreditDecisionMadeEvent.class);
        if (!"APPROVE".equals(event.decision()) || !"PRE_AUCTION".equals(event.stage())) {
            return;
        }
        Instant now = Instant.now();
        Instant expiresAt = now.plusSeconds(windowSeconds);
        Auction auction = new Auction(
            new ProposalId(event.proposalId()),
            new TenantId(event.tenantId()),
            AuctionStatus.OPEN,
            eligibleFunderIds,
            now,
            expiresAt,
            List.of(),
            null
        );

        AuctionOpenedEvent auctionOpened = new AuctionOpenedEvent(
            event.proposalId(), event.tenantId(), eligibleFunderIds, now, expiresAt
        );
        auctionRepository.saveAndPublish(auction, event.proposalId(), "AuctionOpened", auctionOpened);
    }
}
