package com.caas.auction.application;

import com.caas.auction.domain.Auction;
import com.caas.auction.domain.AuctionStatus;
import com.caas.auction.domain.Bid;
import com.caas.auction.domain.WinnerSelector;
import com.caas.events.AuctionClosedEvent;
import java.time.Instant;
import java.util.Optional;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class AuctionCloser {

    private final AuctionRepository auctionRepository;
    private final WinnerSelector winnerSelector = new WinnerSelector();

    public AuctionCloser(AuctionRepository auctionRepository) {
        this.auctionRepository = auctionRepository;
    }

    @Scheduled(fixedDelayString = "${app.auction.closer-fixed-delay-ms}")
    public void closeExpiredAuctions() {
        for (Auction auction : auctionRepository.findOpenExpired(Instant.now())) {
            Optional<Bid> winner = winnerSelector.selectWinner(auction.bids());

            Auction closed = new Auction(
                auction.proposalId(),
                auction.tenantId(),
                winner.isPresent() ? AuctionStatus.CLOSED_WITH_WINNER : AuctionStatus.CLOSED_NO_WINNER,
                auction.eligibleFunderIds(),
                auction.openedAt(),
                auction.expiresAt(),
                auction.bids(),
                winner.orElse(null)
            );

            AuctionClosedEvent event = new AuctionClosedEvent(
                closed.proposalId().value(),
                closed.tenantId().value(),
                closed.status().name(),
                winner.map(Bid::funderId).orElse(null),
                winner.map(Bid::rate).orElse(null)
            );

            auctionRepository.closeAndPublish(closed, closed.proposalId().value(), "AuctionClosed", event);
        }
    }
}
