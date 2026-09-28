package com.caas.auction.infrastructure.outbox;

import static org.assertj.core.api.Assertions.assertThat;

import com.caas.events.CreditDecisionMadeEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.time.Duration;
import java.util.List;
import java.util.Properties;
import java.util.UUID;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.header.Header;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.actuate.observability.AutoConfigureObservability;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.containers.localstack.LocalStackContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureObservability
@Testcontainers
class AuctionTracePropagationIntegrationTest {

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
        registry.add("app.auction.window-seconds", () -> "4");
        registry.add("app.auction.closer-fixed-delay-ms", () -> "200");
    }

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private void publishApprovedDecision(UUID proposalId, String traceId) throws Exception {
        CreditDecisionMadeEvent decision = new CreditDecisionMadeEvent(
            proposalId, UUID.randomUUID(), "APPROVE", 0.95, "PRE_AUCTION", new BigDecimal("5000.00")
        );
        Properties props = new Properties();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, kafka.getBootstrapServers());
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        try (KafkaProducer<String, String> producer = new KafkaProducer<>(props)) {
            ProducerRecord<String, String> record = new ProducerRecord<>(
                "credit.decision.made", proposalId.toString(), objectMapper.writeValueAsString(decision)
            );
            record.headers().add("traceparent", ("00-" + traceId + "-00f067aa0ba902b7-01").getBytes());
            producer.send(record).get();
        }
    }

    private ConsumerRecord<String, String> awaitRecord(String topic, UUID proposalId) {
        Properties props = new Properties();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, kafka.getBootstrapServers());
        props.put(ConsumerConfig.GROUP_ID_CONFIG, "test-group-" + UUID.randomUUID());
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        try (KafkaConsumer<String, String> consumer = new KafkaConsumer<>(props)) {
            consumer.subscribe(List.of(topic));
            long deadline = System.currentTimeMillis() + Duration.ofSeconds(30).toMillis();
            while (System.currentTimeMillis() < deadline) {
                for (ConsumerRecord<String, String> record : consumer.poll(Duration.ofMillis(500))) {
                    if (record.value().contains(proposalId.toString())) {
                        return record;
                    }
                }
            }
            return null;
        }
    }

    private String traceparentOf(ConsumerRecord<String, String> record) {
        Header header = record.headers().lastHeader("traceparent");
        return header == null ? null : new String(header.value());
    }

    @Test
    void auctionOpenedCarriesTheTraceOfTheCreditDecisionThatOpenedIt() throws Exception {
        String traceId = "1af7651916cd43dd8448eb211c80319c";
        UUID proposalId = UUID.randomUUID();

        publishApprovedDecision(proposalId, traceId);

        ConsumerRecord<String, String> opened = awaitRecord("auction.opened", proposalId);
        assertThat(opened).isNotNull();
        assertThat(traceparentOf(opened)).contains(traceId);
    }

    @Test
    void bidPlacedCarriesTheTraceOfTheRequestThatPlacedIt() throws Exception {
        String openingTraceId = "2af7651916cd43dd8448eb211c80319c";
        String bidTraceId = "3bf92f3577b34da6a3ce929d0e0e4736";
        UUID proposalId = UUID.randomUUID();
        publishApprovedDecision(proposalId, openingTraceId);
        assertThat(awaitRecord("auction.opened", proposalId)).isNotNull();

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("traceparent", "00-" + bidTraceId + "-00f067aa0ba902b7-01");
        restTemplate.exchange(
            "http://localhost:" + port + "/auctions/" + proposalId + "/bids",
            HttpMethod.POST,
            new HttpEntity<>("{\"funderId\":\"funder-1\",\"rate\":2.2,\"termMonths\":24}", headers),
            String.class
        );

        ConsumerRecord<String, String> placed = awaitRecord("auction.bid.placed", proposalId);
        assertThat(placed).isNotNull();
        assertThat(traceparentOf(placed)).contains(bidTraceId);
    }

    @Test
    void auctionClosedCarriesTheTraceOfTheCreditDecisionThatOpenedTheAuction() throws Exception {
        String traceId = "4cf92f3577b34da6a3ce929d0e0e4736";
        UUID proposalId = UUID.randomUUID();
        publishApprovedDecision(proposalId, traceId);

        ConsumerRecord<String, String> closed = awaitRecord("auction.closed", proposalId);

        assertThat(closed).isNotNull();
        assertThat(traceparentOf(closed)).contains(traceId);
    }
}
