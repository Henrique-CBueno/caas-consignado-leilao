package com.caas.notification.infrastructure.websocket;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.caas.events.AuctionBidPlacedEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.lang.reflect.Type;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Properties;
import java.util.UUID;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.StringSerializer;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.messaging.converter.MappingJackson2MessageConverter;
import org.springframework.messaging.simp.stomp.StompFrameHandler;
import org.springframework.messaging.simp.stomp.StompHeaders;
import org.springframework.messaging.simp.stomp.StompSession;
import org.springframework.messaging.simp.stomp.StompSessionHandlerAdapter;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.messaging.WebSocketStompClient;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

// Milestone 18: o WebSocket exige o ID token no CONNECT e só entrega eventos do tenant do token.
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
class AuctionEventWebSocketRebroadcastIntegrationTest {

    @Container
    static KafkaContainer kafka = new KafkaContainer(DockerImageName.parse("confluentinc/cp-kafka:7.7.1"));

    @Container
    static GenericContainer<?> cognitoLocal =
        new GenericContainer<>(DockerImageName.parse("jagregory/cognito-local:latest"))
            .withExposedPorts(9229)
            .waitingFor(Wait.forLogMessage(".*Cognito Local running.*\\n", 1));

    private static final UUID TENANT_A = UUID.randomUUID();
    private static final UUID TENANT_B = UUID.randomUUID();
    private static String tokenA;
    private static String tokenB;
    private static String adminToken;

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) throws Exception {
        CognitoLocalFixture cognito =
            new CognitoLocalFixture("http://" + cognitoLocal.getHost() + ":" + cognitoLocal.getMappedPort(9229));
        tokenA = cognito.idTokenFor("a@caas.local", "tenant_id", TENANT_A.toString());
        tokenB = cognito.idTokenFor("b@caas.local", "tenant_id", TENANT_B.toString());
        adminToken = cognito.idTokenFor("admin@caas.local", "role", "admin");
        registry.add("spring.kafka.bootstrap-servers", kafka::getBootstrapServers);
        registry.add("app.cognito.jwk-set-uri", cognito::jwkSetUri);
    }

    @LocalServerPort
    private int port;

    private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

    private StompSession connect(String token) throws Exception {
        WebSocketStompClient stompClient = new WebSocketStompClient(new StandardWebSocketClient());
        MappingJackson2MessageConverter converter = new MappingJackson2MessageConverter();
        converter.getObjectMapper().registerModule(new JavaTimeModule());
        stompClient.setMessageConverter(converter);
        StompHeaders connectHeaders = new StompHeaders();
        if (token != null) {
            connectHeaders.add("Authorization", "Bearer " + token);
        }
        return stompClient
            .connectAsync("ws://localhost:" + port + "/ws", (org.springframework.web.socket.WebSocketHttpHeaders) null, connectHeaders, new StompSessionHandlerAdapter() { })
            .get(5, TimeUnit.SECONDS);
    }

    private BlockingQueue<AuctionNotification> subscribe(StompSession session, UUID tenant, UUID proposalId) {
        BlockingQueue<AuctionNotification> received = new LinkedBlockingQueue<>();
        session.subscribe("/topic/tenants/" + tenant + "/auctions/" + proposalId, new StompFrameHandler() {
            @Override
            public Type getPayloadType(StompHeaders headers) {
                return AuctionNotification.class;
            }

            @Override
            public void handleFrame(StompHeaders headers, Object payload) {
                received.add((AuctionNotification) payload);
            }
        });
        return received;
    }

    private void publishBid(UUID proposalId, UUID tenant) throws Exception {
        AuctionBidPlacedEvent event = new AuctionBidPlacedEvent(
            proposalId, tenant, "funder-alpha", new BigDecimal("1.99"), 24, Instant.now());
        publishToKafka("auction.bid.placed", objectMapper.writeValueAsString(event));
    }

    @Test
    void aConnectionWithoutATokenIsRefused() {
        assertThatThrownBy(() -> connect(null)).isNotNull();
    }

    @Test
    void aConnectionWithAnInvalidTokenIsRefused() {
        assertThatThrownBy(() -> connect("nao.e.um.jwt")).isNotNull();
    }

    @Test
    void anIdentityWithoutATenantIsRefused() {
        assertThatThrownBy(() -> connect(adminToken)).isNotNull();
    }

    @Test
    void aTenantReceivesTheBidsOfItsOwnAuction() throws Exception {
        UUID proposalId = UUID.randomUUID();
        StompSession session = connect(tokenA);
        BlockingQueue<AuctionNotification> received = subscribe(session, TENANT_A, proposalId);

        publishBid(proposalId, TENANT_A);

        AuctionNotification notification = received.poll(15, TimeUnit.SECONDS);
        assertThat(notification).isNotNull();
        assertThat(notification.type()).isEqualTo("BID_PLACED");
    }

    @Test
    void aTenantCannotSubscribeToAnotherTenantsTopic() throws Exception {
        UUID proposalId = UUID.randomUUID();
        StompSession session = connect(tokenA);
        BlockingQueue<AuctionNotification> received = subscribe(session, TENANT_B, proposalId);

        publishBid(proposalId, TENANT_B);

        assertThat(received.poll(5, TimeUnit.SECONDS)).isNull();
    }

    @Test
    void anotherTenantsEventForTheSameProposalIdNeverReachesTheSubscriber() throws Exception {
        UUID proposalId = UUID.randomUUID();
        StompSession session = connect(tokenA);
        BlockingQueue<AuctionNotification> received = subscribe(session, TENANT_A, proposalId);

        publishBid(proposalId, TENANT_B);
        assertThat(received.poll(5, TimeUnit.SECONDS)).isNull();

        publishBid(proposalId, TENANT_A);
        assertThat(received.poll(15, TimeUnit.SECONDS)).isNotNull();
    }

    private void publishToKafka(String topic, String payload) throws Exception {
        Properties producerProps = new Properties();
        producerProps.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, kafka.getBootstrapServers());
        producerProps.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        producerProps.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        try (KafkaProducer<String, String> producer = new KafkaProducer<>(producerProps)) {
            producer.send(new ProducerRecord<>(topic, UUID.randomUUID().toString(), payload)).get();
        }
    }
}
