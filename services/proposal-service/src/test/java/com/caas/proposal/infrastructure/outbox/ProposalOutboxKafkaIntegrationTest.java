package com.caas.proposal.infrastructure.outbox;

import static org.assertj.core.api.Assertions.assertThat;

import com.caas.events.ProposalCreatedEvent;
import com.caas.proposal.infrastructure.web.CreateProposalRequest;
import com.caas.proposal.infrastructure.web.CreateProposalResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.time.Duration;
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
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.kafka.test.utils.KafkaTestUtils;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
class ProposalOutboxKafkaIntegrationTest {

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

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void creatingAProposalPublishesAProposalCreatedEventToKafka() throws Exception {
        UUID tenantId = UUID.randomUUID();
        CreateProposalRequest request = new CreateProposalRequest("12345678900", new BigDecimal("5000.00"), 24);
        HttpHeaders headers = new HttpHeaders();
        headers.set("X-Tenant-Id", tenantId.toString());

        CreateProposalResponse created = restTemplate.exchange(
            "http://localhost:" + port + "/proposals",
            HttpMethod.POST,
            new HttpEntity<>(request, headers),
            CreateProposalResponse.class
        ).getBody();

        Properties consumerProps = new Properties();
        consumerProps.putAll(KafkaTestUtils.consumerProps(kafka.getBootstrapServers(), "test-group-" + UUID.randomUUID(), "true"));
        consumerProps.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        consumerProps.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        consumerProps.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");

        ProposalCreatedEvent event = null;
        try (KafkaConsumer<String, String> consumer = new KafkaConsumer<>(consumerProps)) {
            consumer.subscribe(java.util.List.of("proposal.created"));

            long deadline = System.currentTimeMillis() + Duration.ofSeconds(15).toMillis();
            while (System.currentTimeMillis() < deadline && event == null) {
                ConsumerRecords<String, String> records = consumer.poll(Duration.ofMillis(500));
                for (ConsumerRecord<String, String> record : records) {
                    ProposalCreatedEvent candidate = objectMapper.readValue(record.value(), ProposalCreatedEvent.class);
                    if (candidate.proposalId().equals(created.id())) {
                        event = candidate;
                    }
                }
            }
        }

        assertThat(event).isNotNull();
        assertThat(event.tenantId()).isEqualTo(tenantId);
        assertThat(event.borrowerId()).isEqualTo("12345678900");
        assertThat(event.requestedAmount()).isEqualByComparingTo("5000.00");
        assertThat(event.termMonths()).isEqualTo(24);
    }
}
