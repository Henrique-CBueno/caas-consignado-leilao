package com.caas.credit.infrastructure.kafka;

import static org.assertj.core.api.Assertions.assertThat;

import com.caas.events.AuctionClosedEvent;
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
class AuctionClosedToPostAuctionCreditDecisionMadeIntegrationTest {

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
    void anAuctionClosedWithWinnerProducesAPostAuctionCreditDecisionMadeEvent() throws Exception {
        UUID proposalId = UUID.randomUUID();
        UUID tenantId = UUID.randomUUID();
        // borrowerId "59": score determinístico do MockDecisionAdapter cai na
        // faixa APPROVE/confidence 0.9 (acima do threshold de 0.6).
        ProposalCreatedEvent proposalCreated =
            new ProposalCreatedEvent(proposalId, tenantId, "59", new BigDecimal("5000.00"), 24);
        AuctionClosedEvent auctionClosed =
            new AuctionClosedEvent(proposalId, tenantId, "CLOSED_WITH_WINNER", "funder-2", new BigDecimal("2.08"), 18);

        Properties producerProps = new Properties();
        producerProps.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, kafka.getBootstrapServers());
        producerProps.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        producerProps.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class);

        Properties consumerProps = new Properties();
        consumerProps.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, kafka.getBootstrapServers());
        consumerProps.put(ConsumerConfig.GROUP_ID_CONFIG, "test-group-" + UUID.randomUUID());
        consumerProps.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        consumerProps.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        consumerProps.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");

        CreditDecisionMadeEvent postAuctionEvent;
        try (
            KafkaProducer<String, String> producer = new KafkaProducer<>(producerProps);
            KafkaConsumer<String, String> consumer = new KafkaConsumer<>(consumerProps)
        ) {
            consumer.subscribe(List.of("credit.decision.made"));

            producer.send(new ProducerRecord<>(
                "proposal.created", proposalId.toString(), objectMapper.writeValueAsString(proposalCreated)
            )).get();

            // Espera a decisão pré-leilão ser persistida (evento PRE_AUCTION real) antes de
            // publicar o fechamento — senão a revalidação pode rodar antes de ter o que revalidar.
            pollUntil(consumer, proposalId, "PRE_AUCTION", Duration.ofSeconds(20));

            producer.send(new ProducerRecord<>(
                "auction.closed", proposalId.toString(), objectMapper.writeValueAsString(auctionClosed)
            )).get();

            postAuctionEvent = pollUntil(consumer, proposalId, "POST_AUCTION", Duration.ofSeconds(20));
        }

        assertThat(postAuctionEvent).isNotNull();
        assertThat(postAuctionEvent.tenantId()).isEqualTo(tenantId);
        assertThat(postAuctionEvent.decision()).isEqualTo("APPROVE");
        assertThat(postAuctionEvent.requestedAmount()).isEqualByComparingTo("5000.00");
    }

    private CreditDecisionMadeEvent pollUntil(
        KafkaConsumer<String, String> consumer, UUID proposalId, String stage, Duration timeout
    ) throws Exception {
        long deadline = System.currentTimeMillis() + timeout.toMillis();
        while (System.currentTimeMillis() < deadline) {
            ConsumerRecords<String, String> records = consumer.poll(Duration.ofMillis(500));
            for (ConsumerRecord<String, String> record : records) {
                CreditDecisionMadeEvent candidate =
                    objectMapper.readValue(record.value(), CreditDecisionMadeEvent.class);
                if (candidate.proposalId().equals(proposalId) && stage.equals(candidate.stage())) {
                    return candidate;
                }
            }
        }
        return null;
    }
}
