package com.caas.disbursement.infrastructure.kafka;

import static org.assertj.core.api.Assertions.assertThat;

import com.caas.events.ContractSignedEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Properties;
import java.util.UUID;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.StringSerializer;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
class ContractSignedToDisbursementIntegrationTest {

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
    }

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

    @Test
    void aContractSignedEventProducesADisbursementQueryableByProposalId() throws Exception {
        UUID proposalId = UUID.randomUUID();
        UUID tenantId = UUID.randomUUID();
        ContractSignedEvent contractSigned = new ContractSignedEvent(
            proposalId, tenantId, "funder-2", new BigDecimal("2.08"), 18, new BigDecimal("5000.00"), Instant.now()
        );

        Properties producerProps = new Properties();
        producerProps.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, kafka.getBootstrapServers());
        producerProps.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        producerProps.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        try (KafkaProducer<String, String> producer = new KafkaProducer<>(producerProps)) {
            producer.send(new ProducerRecord<>(
                "contract.signed", proposalId.toString(), objectMapper.writeValueAsString(contractSigned)
            )).get();
        }

        HttpHeaders headers = new HttpHeaders();
        headers.set("X-Tenant-Id", tenantId.toString());

        Map<String, Object> body = null;
        long deadline = System.currentTimeMillis() + Duration.ofSeconds(20).toMillis();
        while (System.currentTimeMillis() < deadline && body == null) {
            ResponseEntity<Map> response = restTemplate.exchange(
                "http://localhost:" + port + "/disbursements/" + proposalId,
                HttpMethod.GET, new HttpEntity<>(headers), Map.class
            );
            if (response.getStatusCode() == HttpStatus.OK) {
                body = response.getBody();
            } else {
                Thread.sleep(500);
            }
        }

        assertThat(body).isNotNull();
        assertThat(body.get("proposalId")).isEqualTo(proposalId.toString());
        assertThat(body.get("funderId")).isEqualTo("funder-2");
        assertThat(new BigDecimal(body.get("amount").toString())).isEqualByComparingTo("5000.00");
        assertThat(body.get("status")).isEqualTo("DISBURSED");
    }

    @Test
    void anUnknownProposalReturnsNotFound() {
        HttpHeaders headers = new HttpHeaders();
        headers.set("X-Tenant-Id", UUID.randomUUID().toString());

        ResponseEntity<Map> response = restTemplate.exchange(
            "http://localhost:" + port + "/disbursements/" + UUID.randomUUID(),
            HttpMethod.GET, new HttpEntity<>(headers), Map.class
        );
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }
}
