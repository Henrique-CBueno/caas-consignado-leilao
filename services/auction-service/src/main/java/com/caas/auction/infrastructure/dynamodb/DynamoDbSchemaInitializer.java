package com.caas.auction.infrastructure.dynamodb;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeDefinition;
import software.amazon.awssdk.services.dynamodb.model.BillingMode;
import software.amazon.awssdk.services.dynamodb.model.CreateTableRequest;
import software.amazon.awssdk.services.dynamodb.model.KeySchemaElement;
import software.amazon.awssdk.services.dynamodb.model.KeyType;
import software.amazon.awssdk.services.dynamodb.model.ResourceInUseException;
import software.amazon.awssdk.services.dynamodb.model.ScalarAttributeType;

// Sem Terraform/migration tooling para DynamoDB neste projeto: cria as tabelas de
// forma idempotente no startup. Suficiente para demo/local; produção real usaria IaC.
@Component
public class DynamoDbSchemaInitializer implements ApplicationRunner {

    private final DynamoDbClient client;

    public DynamoDbSchemaInitializer(DynamoDbClient client) {
        this.client = client;
    }

    @Override
    public void run(ApplicationArguments args) {
        ensureTable("auctions", "proposal_id");
        ensureTable("outbox_events", "id");
    }

    private void ensureTable(String tableName, String partitionKey) {
        try {
            client.createTable(CreateTableRequest.builder()
                .tableName(tableName)
                .billingMode(BillingMode.PAY_PER_REQUEST)
                .attributeDefinitions(AttributeDefinition.builder()
                    .attributeName(partitionKey)
                    .attributeType(ScalarAttributeType.S)
                    .build())
                .keySchema(KeySchemaElement.builder()
                    .attributeName(partitionKey)
                    .keyType(KeyType.HASH)
                    .build())
                .build());
        } catch (ResourceInUseException alreadyExists) {
            // idempotente: tabela já existe, nada a fazer.
        }
    }
}
