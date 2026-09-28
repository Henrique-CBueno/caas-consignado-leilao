package com.caas.auction.infrastructure.persistence;

import com.caas.auction.application.AuctionRepository;
import com.caas.auction.domain.Auction;
import com.caas.observability.TraceContextStore;
import com.caas.auction.domain.AuctionStatus;
import com.caas.auction.domain.Bid;
import com.caas.auction.domain.ProposalId;
import com.caas.auction.domain.TenantId;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Repository;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.GetItemRequest;
import software.amazon.awssdk.services.dynamodb.model.GetItemResponse;
import software.amazon.awssdk.services.dynamodb.model.Put;
import software.amazon.awssdk.services.dynamodb.model.PutItemRequest;
import software.amazon.awssdk.services.dynamodb.model.ScanRequest;
import software.amazon.awssdk.services.dynamodb.model.ScanResponse;
import software.amazon.awssdk.services.dynamodb.model.TransactWriteItem;
import software.amazon.awssdk.services.dynamodb.model.TransactWriteItemsRequest;

@Repository
public class DynamoDbAuctionRepository implements AuctionRepository {

    static final String TABLE_NAME = "auctions";
    static final String OUTBOX_TABLE_NAME = "outbox_events";

    private final DynamoDbClient client;
    private final ObjectMapper objectMapper;
    private final TraceContextStore traceContextStore;

    public DynamoDbAuctionRepository(DynamoDbClient client, ObjectMapper objectMapper, TraceContextStore traceContextStore) {
        this.client = client;
        this.objectMapper = objectMapper;
        this.traceContextStore = traceContextStore;
    }

    @Override
    public Auction save(Auction auction) {
        client.putItem(PutItemRequest.builder().tableName(TABLE_NAME).item(toItem(auction)).build());
        return auction;
    }

    @Override
    public Auction findByProposalId(ProposalId id) {
        GetItemResponse response = client.getItem(GetItemRequest.builder()
            .tableName(TABLE_NAME)
            .key(Map.of("proposal_id", AttributeValue.fromS(id.value().toString())))
            .build());
        if (!response.hasItem() || response.item().isEmpty()) {
            return null;
        }
        return fromItem(response.item());
    }

    @Override
    public String traceparentOf(ProposalId id) {
        GetItemResponse response = client.getItem(GetItemRequest.builder()
            .tableName(TABLE_NAME)
            .key(Map.of("proposal_id", AttributeValue.fromS(id.value().toString())))
            .build());
        return response.hasItem() && response.item().containsKey("traceparent")
            ? response.item().get("traceparent").s()
            : null;
    }

    @Override
    public List<Auction> findOpenExpired(Instant now) {
        ScanResponse response = client.scan(ScanRequest.builder()
            .tableName(TABLE_NAME)
            .filterExpression("#status = :open AND expires_at < :now")
            .expressionAttributeNames(Map.of("#status", "status"))
            .expressionAttributeValues(Map.of(
                ":open", AttributeValue.fromS(AuctionStatus.OPEN.name()),
                ":now", AttributeValue.fromN(String.valueOf(now.toEpochMilli()))
            ))
            .build());
        return response.items().stream().map(this::fromItem).toList();
    }

    @Override
    public void saveAndPublish(Auction auction, UUID aggregateId, String eventType, Object eventPayload) {
        // O leilão guarda o trace de quando foi aberto: o fechamento (scheduler, sem
        // requisição) e os lances precisam reescrever o item sem perder esse contexto.
        String traceparent = traceparentOf(auction.proposalId());
        if (traceparent == null) {
            traceparent = traceContextStore.capture();
        }
        Map<String, AttributeValue> auctionItem = toItem(auction);
        if (traceparent != null) {
            auctionItem.put("traceparent", AttributeValue.fromS(traceparent));
        }
        Put auctionPut = Put.builder().tableName(TABLE_NAME).item(auctionItem).build();

        Map<String, AttributeValue> outboxItem = new HashMap<>();
        outboxItem.put("id", AttributeValue.fromS(UUID.randomUUID().toString()));
        outboxItem.put("aggregate_id", AttributeValue.fromS(aggregateId.toString()));
        outboxItem.put("event_type", AttributeValue.fromS(eventType));
        outboxItem.put("payload", AttributeValue.fromS(writeJson(eventPayload)));
        outboxItem.put("published", AttributeValue.fromBool(false));
        String eventTraceparent = traceContextStore.capture();
        if (eventTraceparent != null) {
            outboxItem.put("traceparent", AttributeValue.fromS(eventTraceparent));
        }
        Put outboxPut = Put.builder().tableName(OUTBOX_TABLE_NAME).item(outboxItem).build();

        // Atomicidade equivalente ao @Transactional dos serviços com Postgres (ADR-0003):
        // aqui, via TransactWriteItems, já que o DynamoDB não tem transação ambiente.
        client.transactWriteItems(TransactWriteItemsRequest.builder()
            .transactItems(
                TransactWriteItem.builder().put(auctionPut).build(),
                TransactWriteItem.builder().put(outboxPut).build()
            )
            .build());
    }

    private Map<String, AttributeValue> toItem(Auction auction) {
        Map<String, AttributeValue> item = new HashMap<>();
        item.put("proposal_id", AttributeValue.fromS(auction.proposalId().value().toString()));
        item.put("tenant_id", AttributeValue.fromS(auction.tenantId().value().toString()));
        item.put("status", AttributeValue.fromS(auction.status().name()));
        item.put("eligible_funder_ids", AttributeValue.fromL(
            auction.eligibleFunderIds().stream().map(AttributeValue::fromS).toList()
        ));
        item.put("opened_at", AttributeValue.fromN(String.valueOf(auction.openedAt().toEpochMilli())));
        item.put("expires_at", AttributeValue.fromN(String.valueOf(auction.expiresAt().toEpochMilli())));
        item.put("bids_json", AttributeValue.fromS(writeJson(auction.bids())));
        if (auction.winningBid() != null) {
            item.put("winning_bid_json", AttributeValue.fromS(writeJson(auction.winningBid())));
        }
        return item;
    }

    private Auction fromItem(Map<String, AttributeValue> item) {
        Bid winningBid = item.containsKey("winning_bid_json")
            ? readJson(item.get("winning_bid_json").s(), Bid.class)
            : null;
        return new Auction(
            new ProposalId(UUID.fromString(item.get("proposal_id").s())),
            new TenantId(UUID.fromString(item.get("tenant_id").s())),
            AuctionStatus.valueOf(item.get("status").s()),
            item.get("eligible_funder_ids").l().stream().map(AttributeValue::s).toList(),
            Instant.ofEpochMilli(Long.parseLong(item.get("opened_at").n())),
            Instant.ofEpochMilli(Long.parseLong(item.get("expires_at").n())),
            readJson(item.get("bids_json").s(), new TypeReference<List<Bid>>() { }),
            winningBid
        );
    }

    private String writeJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Falha ao serializar item do leilão", e);
        }
    }

    private <T> T readJson(String json, Class<T> type) {
        try {
            return objectMapper.readValue(json, type);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Falha ao desserializar item do leilão", e);
        }
    }

    private <T> T readJson(String json, TypeReference<T> type) {
        try {
            return objectMapper.readValue(json, type);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Falha ao desserializar item do leilão", e);
        }
    }
}
