package com.caas.auction.infrastructure.outbox;

import static org.assertj.core.api.Assertions.assertThat;

import com.caas.auction.application.AuctionRepository;
import com.caas.auction.domain.Auction;
import com.caas.auction.domain.AuctionStatus;
import com.caas.auction.domain.ProposalId;
import com.caas.auction.domain.TenantId;
import com.caas.auction.infrastructure.web.BidRequest;
import com.caas.events.AuctionBidPlacedEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Properties;
import java.util.UUID;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.kafka.test.utils.KafkaTestUtils;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.containers.localstack.LocalStackContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
class AuctionBidPlacedOutboxKafkaIntegrationTest {

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

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private AuctionRepository auctionRepository;

    private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

    @Test
    void placingABidPublishesAnAuctionBidPlacedEventToKafka() throws Exception {
        ProposalId proposalId = new ProposalId(UUID.randomUUID());
        Instant now = Instant.now().truncatedTo(ChronoUnit.MILLIS);
        Auction openAuction = new Auction(
            proposalId,
            new TenantId(UUID.randomUUID()),
            AuctionStatus.OPEN,
            List.of("funder-alpha", "funder-beta"),
            now,
            now.plusSeconds(60),
            List.of(),
            null
        );
        auctionRepository.save(openAuction);

        BidRequest request = new BidRequest("funder-alpha", new BigDecimal("1.99"), 24);
        restTemplate.postForEntity(
            "http://localhost:" + port + "/auctions/" + proposalId.value() + "/bids",
            request,
            Void.class
        );

        Properties consumerProps = new Properties();
        consumerProps.putAll(KafkaTestUtils.consumerProps(kafka.getBootstrapServers(), "test-group-" + UUID.randomUUID(), "true"));
        consumerProps.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        consumerProps.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        consumerProps.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");

        AuctionBidPlacedEvent event = null;
        try (KafkaConsumer<String, String> consumer = new KafkaConsumer<>(consumerProps)) {
            consumer.subscribe(List.of("auction.bid.placed"));

            long deadline = System.currentTimeMillis() + Duration.ofSeconds(15).toMillis();
            while (System.currentTimeMillis() < deadline && event == null) {
                ConsumerRecords<String, String> records = consumer.poll(Duration.ofMillis(500));
                for (ConsumerRecord<String, String> record : records) {
                    AuctionBidPlacedEvent candidate = objectMapper.readValue(record.value(), AuctionBidPlacedEvent.class);
                    if (candidate.proposalId().equals(proposalId.value())) {
                        event = candidate;
                    }
                }
            }
        }

        assertThat(event).isNotNull();
        assertThat(event.funderId()).isEqualTo("funder-alpha");
        assertThat(event.rate()).isEqualByComparingTo("1.99");
        assertThat(event.termMonths()).isEqualTo(24);
    }
}
