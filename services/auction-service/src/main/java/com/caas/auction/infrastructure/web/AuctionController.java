package com.caas.auction.infrastructure.web;

import com.caas.auction.application.AuctionRepository;
import com.caas.auction.domain.Auction;
import com.caas.auction.domain.AuctionStatus;
import com.caas.auction.domain.Bid;
import com.caas.auction.domain.ProposalId;
import com.caas.events.AuctionBidPlacedEvent;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

// Sem lock otimista: leituras/escritas concorrentes de lance podem se perder
// (last-write-wins) — aceitável no volume de demo, registrado como simplificação
// consciente na spec da Milestone 5. Correção de verdade usaria update condicional
// do DynamoDB (attribute_exists + status = OPEN) com bids como lista nativa, não JSON.
@RestController
public class AuctionController {

    private final AuctionRepository auctionRepository;

    public AuctionController(AuctionRepository auctionRepository) {
        this.auctionRepository = auctionRepository;
    }

    @Operation(summary = "Registra um lance num leilão aberto", description = "Uso interno (funder-bot-service). Publica AuctionBidPlaced via outbox.", responses = {
        @ApiResponse(responseCode = "200", description = "Lance aceito; devolve o leilão atualizado"),
        @ApiResponse(responseCode = "404", description = "Leilão inexistente", content = @io.swagger.v3.oas.annotations.media.Content),
        @ApiResponse(responseCode = "409", description = "Leilão já fechado ou expirado", content = @io.swagger.v3.oas.annotations.media.Content)
    })
    @PostMapping("/auctions/{proposalId}/bids")
    public AuctionResponse placeBid(@PathVariable UUID proposalId, @RequestBody BidRequest request) {
        Auction auction = auctionRepository.findByProposalId(new ProposalId(proposalId));
        if (auction == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
        if (auction.status() != AuctionStatus.OPEN || Instant.now().isAfter(auction.expiresAt())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Leilão não está aberto para lances");
        }

        Instant receivedAt = Instant.now();
        List<Bid> bids = new ArrayList<>(auction.bids());
        bids.add(new Bid(request.funderId(), request.rate(), request.termMonths(), receivedAt));

        Auction updated = new Auction(
            auction.proposalId(), auction.tenantId(), auction.status(), auction.eligibleFunderIds(),
            auction.openedAt(), auction.expiresAt(), bids, auction.winningBid()
        );

        AuctionBidPlacedEvent event = new AuctionBidPlacedEvent(
            updated.proposalId().value(), updated.tenantId().value(),
            request.funderId(), request.rate(), request.termMonths(), receivedAt
        );
        auctionRepository.saveAndPublish(updated, updated.proposalId().value(), "AuctionBidPlaced", event);
        return toResponse(updated);
    }

    @Operation(summary = "Consulta um leilão pelo id da proposta", responses = {
        @ApiResponse(responseCode = "200", description = "Leilão encontrado"),
        @ApiResponse(responseCode = "404", description = "Leilão inexistente", content = @io.swagger.v3.oas.annotations.media.Content)
    })
    @GetMapping("/auctions/{proposalId}")
    public ResponseEntity<AuctionResponse> getAuction(@PathVariable UUID proposalId) {
        Auction auction = auctionRepository.findByProposalId(new ProposalId(proposalId));
        if (auction == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(toResponse(auction));
    }

    private AuctionResponse toResponse(Auction auction) {
        return new AuctionResponse(
            auction.proposalId().value(),
            auction.status().name(),
            auction.bids(),
            auction.winningBid() != null ? auction.winningBid().funderId() : null
        );
    }
}
