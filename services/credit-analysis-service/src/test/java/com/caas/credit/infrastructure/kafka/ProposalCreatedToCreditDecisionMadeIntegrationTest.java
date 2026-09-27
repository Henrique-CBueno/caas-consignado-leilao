package com.caas.credit.infrastructure.kafka;

import static org.assertj.core.api.Assertions.assertThat;

import com.caas.events.CreditDecisionMadeEvent;
import com.caas.events.ProposalCreatedEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.time.Duration;
import java.util.List;
import java.util.Properties;
import java.util.UUID;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

@SpringBootTest
@Testcontainers
class ProposalCreatedToCreditDecisionMadeIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @Container
    static KafkaContainer kafka = new KafkaContainer(DockerImageName.parse("confluentinc/cp-kafka:7.7.1"));

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.kafka.bootstrap-servers", kafka::getBootstrapServers);
        registry.add("app.outbox.relay-fixed-delay-ms", () -> "200");
    }

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void aProposalCreatedEventProducesACreditDecisionMadeEvent() throws Exception {
        UUID proposalId = UUID.randomUUID();
        UUID tenantId = UUID.randomUUID();
        ProposalCreatedEvent proposalCreated = new ProposalCreatedEvent(
            proposalId, tenantId, "12345678900", new BigDecimal("5000.00"), 24
        );

        Properties producerProps = new Properties();
        producerProps.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, kafka.getBootstrapServers());
        producerProps.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        producerProps.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        try (KafkaProducer<String, String> producer = new KafkaProducer<>(producerProps)) {
            producer.send(new ProducerRecord<>(
                "proposal.created",
                proposalId.toString(),
                objectMapper.writeValueAsString(proposalCreated)
            )).get();
        }

        Properties consumerProps = new Properties();
        consumerProps.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, kafka.getBootstrapServers());
        consumerProps.put(ConsumerConfig.GROUP_ID_CONFIG, "test-group-" + UUID.randomUUID());
        consumerProps.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        consumerProps.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        consumerProps.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");

        CreditDecisionMadeEvent event = null;
        try (KafkaConsumer<String, String> consumer = new KafkaConsumer<>(consumerProps)) {
            consumer.subscribe(List.of("credit.decision.made"));

            long deadline = System.currentTimeMillis() + Duration.ofSeconds(20).toMillis();
            while (System.currentTimeMillis() < deadline && event == null) {
                ConsumerRecords<String, String> records = consumer.poll(Duration.ofMillis(500));
                for (ConsumerRecord<String, String> record : records) {
                    CreditDecisionMadeEvent candidate = objectMapper.readValue(record.value(), CreditDecisionMadeEvent.class);
                    if (candidate.proposalId().equals(proposalId)) {
                        event = candidate;
                    }
                }
            }
        }

        assertThat(event).isNotNull();
        assertThat(event.tenantId()).isEqualTo(tenantId);
        assertThat(event.decision()).isIn("APPROVE", "REJECT", "MANUAL_REVIEW");
    }
}
