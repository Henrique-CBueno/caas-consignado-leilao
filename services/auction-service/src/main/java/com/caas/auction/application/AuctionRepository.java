package com.caas.auction.application;

import com.caas.auction.domain.Auction;
import com.caas.auction.domain.ProposalId;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface AuctionRepository {
    Auction save(Auction auction);

    Auction findByProposalId(ProposalId id);

    // W3C traceparent do momento em que o leilão foi aberto (null se não havia trace).
    String traceparentOf(ProposalId id);

    List<Auction> findOpenExpired(Instant now);

    // Sem transação ambiente (@Transactional) como nos serviços com Postgres: o
    // DynamoDB exige coordenar a escrita do agregado + o evento de outbox
    // explicitamente via TransactWriteItems (ver DynamoDbAuctionRepository e
    // ADR do outbox nesta milestone). Usado tanto ao fechar o leilão quanto ao
    // aceitar um lance — mesma mecânica nos dois casos.
    void saveAndPublish(Auction auction, UUID aggregateId, String eventType, Object eventPayload);
}
