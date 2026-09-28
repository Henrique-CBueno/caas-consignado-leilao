package com.caas.funderbot.infrastructure.kafka;

import static org.assertj.core.api.Assertions.assertThat;

import com.caas.events.AuctionOpenedEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.sun.net.httpserver.HttpServer;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.StringSerializer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.actuate.observability.AutoConfigureObservability;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

@SpringBootTest
@AutoConfigureObservability
@Testcontainers
class AuctionOpenedBidSubmissionIntegrationTest {

    @Container
    static KafkaContainer kafka = new KafkaContainer(DockerImageName.parse("confluentinc/cp-kafka:7.7.1"));

    static HttpServer auctionServiceStub;
    static final List<String> receivedPaths = new CopyOnWriteArrayList<>();
    static final List<String> receivedBodies = new CopyOnWriteArrayList<>();
    static final List<String> receivedTraceparents = new CopyOnWriteArrayList<>();

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) throws Exception {
        registry.add("spring.kafka.bootstrap-servers", kafka::getBootstrapServers);
        registry.add("app.funder-bot.min-delay-ms", () -> "1");
        registry.add("app.funder-bot.max-delay-ms", () -> "50");

        auctionServiceStub = HttpServer.create(new InetSocketAddress(0), 0);
        auctionServiceStub.createContext("/", exchange -> {
            receivedPaths.add(exchange.getRequestURI().toString());
            receivedTraceparents.add(String.valueOf(exchange.getRequestHeaders().getFirst("traceparent")));
            receivedBodies.add(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            exchange.sendResponseHeaders(200, -1);
            exchange.close();
        });
        auctionServiceStub.start();
        registry.add("app.auction-service.base-url",
            () -> "http://localhost:" + auctionServiceStub.getAddress().getPort());
    }

    @BeforeEach
    void clearReceived() {
        receivedPaths.clear();
        receivedBodies.clear();
        receivedTraceparents.clear();
    }

    @AfterEach
    void stopStub() {
        // servidor fica de pé entre os testes da classe (porta fixa na property estática);
        // apenas o estado observado é limpo em @BeforeEach.
    }

    private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

    @Test
    void eachEligibleBotSubmitsABidWhenAnAuctionOpens() throws Exception {
        UUID proposalId = UUID.randomUUID();
        Instant now = Instant.now();
        AuctionOpenedEvent auctionOpened = new AuctionOpenedEvent(
            proposalId, UUID.randomUUID(),
            List.of("funder-1", "funder-2", "funder-3"),
            now, now.plusSeconds(45)
        );

        publishAuctionOpened(auctionOpened);

        long deadline = System.currentTimeMillis() + Duration.ofSeconds(15).toMillis();
        while (System.currentTimeMillis() < deadline && receivedPaths.size() < 3) {
            Thread.sleep(200);
        }

        assertThat(receivedPaths).hasSize(3);
        assertThat(receivedPaths).allMatch(path -> path.equals("/auctions/" + proposalId + "/bids"));

        Set<String> funderIdsBid = receivedBodies.stream()
            .map(this::readFunderId)
            .collect(java.util.stream.Collectors.toSet());
        assertThat(funderIdsBid).containsExactlyInAnyOrder("funder-1", "funder-2", "funder-3");
    }

    @Test
    void eachBidCarriesTheTraceOfTheAuctionOpenedEventThatTriggeredIt() throws Exception {
        String traceId = "6df92f3577b34da6a3ce929d0e0e4736";
        UUID proposalId = UUID.randomUUID();
        Instant now = Instant.now();
        AuctionOpenedEvent auctionOpened = new AuctionOpenedEvent(
            proposalId, UUID.randomUUID(),
            List.of("funder-1", "funder-2", "funder-3"),
            now, now.plusSeconds(45)
        );

        publishAuctionOpened(auctionOpened, traceId);

        long deadline = System.currentTimeMillis() + Duration.ofSeconds(15).toMillis();
        while (System.currentTimeMillis() < deadline && receivedTraceparents.size() < 3) {
            Thread.sleep(200);
        }

        assertThat(receivedTraceparents).hasSize(3);
        assertThat(receivedTraceparents).allMatch(traceparent -> traceparent.contains(traceId));
    }

    @Test
    void noBidIsSubmittedWhenNoConfiguredBotIsEligible() throws Exception {
        UUID proposalId = UUID.randomUUID();
        Instant now = Instant.now();
        AuctionOpenedEvent auctionOpened = new AuctionOpenedEvent(
            proposalId, UUID.randomUUID(),
            List.of("funder-not-configured"),
            now, now.plusSeconds(45)
        );

        publishAuctionOpened(auctionOpened);

        Thread.sleep(1000);

        assertThat(receivedPaths).isEmpty();
    }

    private void publishAuctionOpened(AuctionOpenedEvent event) throws Exception {
        publishAuctionOpened(event, null);
    }

    private void publishAuctionOpened(AuctionOpenedEvent event, String traceId) throws Exception {
        Properties producerProps = new Properties();
        producerProps.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, kafka.getBootstrapServers());
        producerProps.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        producerProps.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        try (KafkaProducer<String, String> producer = new KafkaProducer<>(producerProps)) {
            ProducerRecord<String, String> record = new ProducerRecord<>(
                "auction.opened", event.proposalId().toString(), objectMapper.writeValueAsString(event)
            );
            if (traceId != null) {
                record.headers().add("traceparent", ("00-" + traceId + "-00f067aa0ba902b7-01").getBytes());
            }
            producer.send(record).get();
        }
    }

    private String readFunderId(String body) {
        try {
            ObjectNode node = (ObjectNode) objectMapper.readTree(body);
            return node.get("funderId").asText();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
