package com.caas.contract.infrastructure.kafka;

import static org.assertj.core.api.Assertions.assertThat;

import com.caas.events.AuctionClosedEvent;
import com.caas.events.ContractSignedEvent;
import com.caas.events.CreditDecisionMadeEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
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
class ContractSignedIntegrationTest {

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

    private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

    @Test
    void bothEventsInEitherOrderProduceExactlyOneContractSigned() throws Exception {
        UUID proposalId = UUID.randomUUID();
        UUID tenantId = UUID.randomUUID();
        CreditDecisionMadeEvent postAuctionApproved = new CreditDecisionMadeEvent(
            proposalId, tenantId, "APPROVE", 0.9, "POST_AUCTION", new BigDecimal("5000.00")
        );
        AuctionClosedEvent auctionClosed = new AuctionClosedEvent(
            proposalId, tenantId, "CLOSED_WITH_WINNER", "funder-2", new BigDecimal("2.08"), 18
        );

        Properties producerProps = producerProps();
        try (KafkaProducer<String, String> producer = new KafkaProducer<>(producerProps)) {
            // Ordem deliberadamente "leilão primeiro, decisão depois" — o contrato
            // não pode depender de qual dos dois chega antes.
            producer.send(new ProducerRecord<>(
                "auction.closed", proposalId.toString(), objectMapper.writeValueAsString(auctionClosed)
            )).get();
            producer.send(new ProducerRecord<>(
                "credit.decision.made", proposalId.toString(), objectMapper.writeValueAsString(postAuctionApproved)
            )).get();
        }

        ContractSignedEvent contractSigned = pollForContractSigned(proposalId, Duration.ofSeconds(20));

        assertThat(contractSigned).isNotNull();
        assertThat(contractSigned.tenantId()).isEqualTo(tenantId);
        assertThat(contractSigned.funderId()).isEqualTo("funder-2");
        assertThat(contractSigned.rate()).isEqualByComparingTo("2.08");
        assertThat(contractSigned.termMonths()).isEqualTo(18);
        assertThat(contractSigned.amount()).isEqualByComparingTo("5000.00");
    }

    @Test
    void onlyOneOfTheTwoEventsProducesNoContract() throws Exception {
        UUID proposalId = UUID.randomUUID();
        UUID tenantId = UUID.randomUUID();
        AuctionClosedEvent auctionClosed = new AuctionClosedEvent(
            proposalId, tenantId, "CLOSED_WITH_WINNER", "funder-1", new BigDecimal("2.5"), 24
        );

        try (KafkaProducer<String, String> producer = new KafkaProducer<>(producerProps())) {
            producer.send(new ProducerRecord<>(
                "auction.closed", proposalId.toString(), objectMapper.writeValueAsString(auctionClosed)
            )).get();
        }

        ContractSignedEvent contractSigned = pollForContractSigned(proposalId, Duration.ofSeconds(5));

        assertThat(contractSigned).isNull();
    }

    @Test
    void aRejectedPostAuctionDecisionNeverProducesAContract() throws Exception {
        UUID proposalId = UUID.randomUUID();
        UUID tenantId = UUID.randomUUID();
        CreditDecisionMadeEvent postAuctionRejected = new CreditDecisionMadeEvent(
            proposalId, tenantId, "REJECT", 0.85, "POST_AUCTION", new BigDecimal("5000.00")
        );
        AuctionClosedEvent auctionClosed = new AuctionClosedEvent(
            proposalId, tenantId, "CLOSED_WITH_WINNER", "funder-3", new BigDecimal("2.2"), 12
        );

        try (KafkaProducer<String, String> producer = new KafkaProducer<>(producerProps())) {
            producer.send(new ProducerRecord<>(
                "credit.decision.made", proposalId.toString(), objectMapper.writeValueAsString(postAuctionRejected)
            )).get();
            producer.send(new ProducerRecord<>(
                "auction.closed", proposalId.toString(), objectMapper.writeValueAsString(auctionClosed)
            )).get();
        }

        ContractSignedEvent contractSigned = pollForContractSigned(proposalId, Duration.ofSeconds(5));

        assertThat(contractSigned).isNull();
    }

    private Properties producerProps() {
        Properties producerProps = new Properties();
        producerProps.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, kafka.getBootstrapServers());
        producerProps.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        producerProps.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        return producerProps;
    }

    private ContractSignedEvent pollForContractSigned(UUID proposalId, Duration timeout) throws Exception {
        Properties consumerProps = new Properties();
        consumerProps.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, kafka.getBootstrapServers());
        consumerProps.put(ConsumerConfig.GROUP_ID_CONFIG, "test-group-" + UUID.randomUUID());
        consumerProps.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        consumerProps.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        consumerProps.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");

        try (KafkaConsumer<String, String> consumer = new KafkaConsumer<>(consumerProps)) {
            consumer.subscribe(List.of("contract.signed"));

            long deadline = System.currentTimeMillis() + timeout.toMillis();
            while (System.currentTimeMillis() < deadline) {
                ConsumerRecords<String, String> records = consumer.poll(Duration.ofMillis(500));
                for (ConsumerRecord<String, String> record : records) {
                    ContractSignedEvent candidate = objectMapper.readValue(record.value(), ContractSignedEvent.class);
                    if (candidate.proposalId().equals(proposalId)) {
                        return candidate;
                    }
                }
            }
            return null;
        }
    }
}
