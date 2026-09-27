package com.caas.auction.infrastructure.outbox;

import java.util.Map;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.ScanRequest;
import software.amazon.awssdk.services.dynamodb.model.ScanResponse;
import software.amazon.awssdk.services.dynamodb.model.UpdateItemRequest;

// Scan simples (não GSI) na tabela de outbox: volume de demo é baixo — ver spec
// da Milestone 5. Mesmo papel do OutboxRelay dos serviços com Postgres, adaptado
// à API do DynamoDB (sem @Transactional/@Scheduled sobre uma transação JPA).
@Component
public class OutboxRelay {

    private static final String OUTBOX_TABLE_NAME = "outbox_events";

    private final DynamoDbClient client;
    private final KafkaTemplate<String, String> kafkaTemplate;

    public OutboxRelay(DynamoDbClient client, KafkaTemplate<String, String> kafkaTemplate) {
        this.client = client;
        this.kafkaTemplate = kafkaTemplate;
    }

    @Scheduled(fixedDelayString = "${app.outbox.relay-fixed-delay-ms}")
    public void relayPendingEvents() {
        ScanResponse response = client.scan(ScanRequest.builder()
            .tableName(OUTBOX_TABLE_NAME)
            .filterExpression("published = :false")
            .expressionAttributeValues(Map.of(":false", AttributeValue.fromBool(false)))
            .build());

        for (Map<String, AttributeValue> item : response.items()) {
            String id = item.get("id").s();
            String eventType = item.get("event_type").s();
            String aggregateId = item.get("aggregate_id").s();
            String payload = item.get("payload").s();

            try {
                kafkaTemplate.send(topicFor(eventType), aggregateId, payload).get();
            } catch (Exception e) {
                throw new IllegalStateException("Falha ao publicar evento de outbox " + id, e);
            }

            client.updateItem(UpdateItemRequest.builder()
                .tableName(OUTBOX_TABLE_NAME)
                .key(Map.of("id", AttributeValue.fromS(id)))
                .updateExpression("SET published = :true")
                .expressionAttributeValues(Map.of(":true", AttributeValue.fromBool(true)))
                .build());
        }
    }

    private String topicFor(String eventType) {
        if ("AuctionClosed".equals(eventType)) {
            return "auction.closed";
        }
        throw new IllegalStateException("Sem tópico Kafka mapeado para o evento '" + eventType + "'");
    }
}
