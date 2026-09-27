package com.caas.auction.infrastructure.kafka;

import static org.assertj.core.api.Assertions.assertThat;

import com.caas.events.AuctionOpenedEvent;
import com.caas.events.CreditDecisionMadeEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
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
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.containers.localstack.LocalStackContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
class CreditDecisionMadeToAuctionOpenedIntegrationTest {

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    @Container
    static LocalStackContainer localstack =
        new LocalStackContainer(DockerImageName.parse("localstack/localstack:3.8"))
            .withServices(LocalStackContainer.Service.DYNAMODB);

    @Container
    static KafkaContainer kafka = new KafkaContainer(DockerImageName.parse("confluentinc/cp-kafka:7.7.1"));

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add("app.dynamodb.endpoint",
            () -> localstack.getEndpointOverride(LocalStackContainer.Service.DYNAMODB).toString());
        registry.add("app.dynamodb.region", localstack::getRegion);
        registry.add("app.dynamodb.access-key", localstack::getAccessKey);
        registry.add("app.dynamodb.secret-key", localstack::getSecretKey);
        registry.add("spring.kafka.bootstrap-servers", kafka::getBootstrapServers);
        registry.add("app.outbox.relay-fixed-delay-ms", () -> "200");
    }

    private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

    @Test
    void anApprovedCreditDecisionPublishesAnAuctionOpenedEventToKafka() throws Exception {
        UUID proposalId = UUID.randomUUID();
        UUID tenantId = UUID.randomUUID();
        CreditDecisionMadeEvent creditDecisionMade = new CreditDecisionMadeEvent(
            proposalId, tenantId, "APPROVE", 0.95, "PRE_AUCTION", new java.math.BigDecimal("5000.00")
        );

        Properties producerProps = new Properties();
        producerProps.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, kafka.getBootstrapServers());
        producerProps.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        producerProps.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        try (KafkaProducer<String, String> producer = new KafkaProducer<>(producerProps)) {
            producer.send(new ProducerRecord<>(
                "credit.decision.made",
                proposalId.toString(),
                objectMapper.writeValueAsString(creditDecisionMade)
            )).get();
        }

        Properties consumerProps = new Properties();
        consumerProps.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, kafka.getBootstrapServers());
        consumerProps.put(ConsumerConfig.GROUP_ID_CONFIG, "test-group-" + UUID.randomUUID());
        consumerProps.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        consumerProps.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        consumerProps.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");

        AuctionOpenedEvent event = null;
        try (KafkaConsumer<String, String> consumer = new KafkaConsumer<>(consumerProps)) {
            consumer.subscribe(List.of("auction.opened"));

            long deadline = System.currentTimeMillis() + Duration.ofSeconds(20).toMillis();
            while (System.currentTimeMillis() < deadline && event == null) {
                ConsumerRecords<String, String> records = consumer.poll(Duration.ofMillis(500));
                for (ConsumerRecord<String, String> record : records) {
                    AuctionOpenedEvent candidate = objectMapper.readValue(record.value(), AuctionOpenedEvent.class);
                    if (candidate.proposalId().equals(proposalId)) {
                        event = candidate;
                    }
                }
            }
        }

        assertThat(event).isNotNull();
        assertThat(event.tenantId()).isEqualTo(tenantId);
        assertThat(event.eligibleFunderIds()).isNotEmpty();
    }

    @Test
    void aPostAuctionCreditDecisionDoesNotOpenASecondAuction() throws Exception {
        UUID proposalId = UUID.randomUUID();
        UUID tenantId = UUID.randomUUID();
        CreditDecisionMadeEvent postAuctionDecision = new CreditDecisionMadeEvent(
            proposalId, tenantId, "APPROVE", 0.95, "POST_AUCTION", new java.math.BigDecimal("5000.00")
        );

        Properties producerProps = new Properties();
        producerProps.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, kafka.getBootstrapServers());
        producerProps.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        producerProps.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        try (KafkaProducer<String, String> producer = new KafkaProducer<>(producerProps)) {
            producer.send(new ProducerRecord<>(
                "credit.decision.made",
                proposalId.toString(),
                objectMapper.writeValueAsString(postAuctionDecision)
            )).get();
        }

        Thread.sleep(3000);

        var response = restTemplate.getForEntity(
            "http://localhost:" + port + "/auctions/" + proposalId, String.class
        );
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }
}
