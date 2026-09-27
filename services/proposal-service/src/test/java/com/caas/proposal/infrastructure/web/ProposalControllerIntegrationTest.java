package com.caas.proposal.infrastructure.web;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
class ProposalControllerIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    private HttpHeaders headers(UUID tenantId) {
        HttpHeaders headers = new HttpHeaders();
        headers.set("X-Tenant-Id", tenantId.toString());
        return headers;
    }

    private CreateProposalResponse createProposal(UUID tenantId) {
        CreateProposalRequest request = new CreateProposalRequest("12345678900", new BigDecimal("5000.00"), 24);
        ResponseEntity<CreateProposalResponse> response = restTemplate.exchange(
            "http://localhost:" + port + "/proposals",
            HttpMethod.POST,
            new HttpEntity<>(request, headers(tenantId)),
            CreateProposalResponse.class
        );
        return response.getBody();
    }

    @Test
    void aTenantCanCreateAProposalAndRetrieveItBack() {
        UUID tenantId = UUID.randomUUID();

        CreateProposalResponse created = createProposal(tenantId);

        ResponseEntity<CreateProposalResponse> response = restTemplate.exchange(
            "http://localhost:" + port + "/proposals/" + created.id(),
            HttpMethod.GET,
            new HttpEntity<Void>(headers(tenantId)),
            CreateProposalResponse.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().borrowerId()).isEqualTo("12345678900");
        assertThat(response.getBody().status()).isEqualTo("PENDING_CREDIT_ANALYSIS");
    }

    @Test
    void creatingAProposalWithANonPositiveRequestedAmountIsRejected() {
        CreateProposalRequest request = new CreateProposalRequest("12345678900", BigDecimal.ZERO, 24);

        ResponseEntity<String> response = restTemplate.exchange(
            "http://localhost:" + port + "/proposals",
            HttpMethod.POST,
            new HttpEntity<>(request, headers(UUID.randomUUID())),
            String.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void aTenantCannotRetrieveAnotherTenantsProposal() {
        UUID tenantA = UUID.randomUUID();
        UUID tenantB = UUID.randomUUID();
        CreateProposalResponse created = createProposal(tenantA);

        ResponseEntity<String> response = restTemplate.exchange(
            "http://localhost:" + port + "/proposals/" + created.id(),
            HttpMethod.GET,
            new HttpEntity<Void>(headers(tenantB)),
            String.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }
}
