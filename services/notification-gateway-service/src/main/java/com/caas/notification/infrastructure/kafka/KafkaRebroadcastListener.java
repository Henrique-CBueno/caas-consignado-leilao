package com.caas.notification.infrastructure.kafka;

import com.caas.events.AuctionBidPlacedEvent;
import com.caas.events.AuctionClosedEvent;
import com.caas.events.AuctionOpenedEvent;
import com.caas.notification.infrastructure.websocket.AuctionNotification;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.UUID;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

@Component
public class KafkaRebroadcastListener {

    private final SimpMessagingTemplate messagingTemplate;
    private final ObjectMapper objectMapper;

    public KafkaRebroadcastListener(SimpMessagingTemplate messagingTemplate, ObjectMapper objectMapper) {
        this.messagingTemplate = messagingTemplate;
        this.objectMapper = objectMapper;
    }

    // Milestone 20: a abertura traz o prazo (expiresAt) que o dashboard usa na contagem regressiva.
    @KafkaListener(topics = "auction.opened")
    public void onAuctionOpened(String payload) throws Exception {
        AuctionOpenedEvent event = objectMapper.readValue(payload, AuctionOpenedEvent.class);
        broadcast(event.tenantId(), event.proposalId(), new AuctionNotification("AUCTION_OPENED", event));
    }

    @KafkaListener(topics = "auction.bid.placed")
    public void onBidPlaced(String payload) throws Exception {
        AuctionBidPlacedEvent event = objectMapper.readValue(payload, AuctionBidPlacedEvent.class);
        broadcast(event.tenantId(), event.proposalId(), new AuctionNotification("BID_PLACED", event));
    }

    @KafkaListener(topics = "auction.closed")
    public void onAuctionClosed(String payload) throws Exception {
        AuctionClosedEvent event = objectMapper.readValue(payload, AuctionClosedEvent.class);
        broadcast(event.tenantId(), event.proposalId(), new AuctionNotification("AUCTION_CLOSED", event));
    }

    // Tópico por tenant (Milestone 18): só o tenant dono do leilão assina este destino.
    private void broadcast(UUID tenantId, UUID proposalId, AuctionNotification notification) {
        messagingTemplate.convertAndSend("/topic/tenants/" + tenantId + "/auctions/" + proposalId, notification);
    }
}
