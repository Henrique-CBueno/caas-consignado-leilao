package com.caas.proposal.infrastructure.web;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
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

// Milestone 19: listagem das propostas do tenant, da mais recente para a mais antiga, paginada.
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
class ProposalListIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    private HttpHeaders headers(UUID tenantId) {
        HttpHeaders headers = new HttpHeaders();
        headers.set("X-Tenant-Id", tenantId.toString());
        return headers;
    }

    private String createProposal(UUID tenantId, String borrower) throws Exception {
        Thread.sleep(5); // created_at distinto e ordenável
        ResponseEntity<String> response = restTemplate.exchange(
            "http://localhost:" + port + "/proposals",
            HttpMethod.POST,
            new HttpEntity<>(new CreateProposalRequest(borrower, new BigDecimal("5000.00"), 24), headers(tenantId)),
            String.class
        );
        return MAPPER.readTree(response.getBody()).path("id").asText();
    }

    private JsonNode list(UUID tenantId, String query) throws Exception {
        ResponseEntity<String> response = restTemplate.exchange(
            "http://localhost:" + port + "/proposals" + query,
            HttpMethod.GET,
            new HttpEntity<Void>(headers(tenantId)),
            String.class
        );
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        return MAPPER.readTree(response.getBody());
    }

    private static List<String> ids(JsonNode page) {
        List<String> ids = new ArrayList<>();
        page.path("items").forEach(item -> ids.add(item.path("id").asText()));
        return ids;
    }

    @Test
    void aTenantListsOnlyItsOwnProposalsNewestFirst() throws Exception {
        UUID tenantA = UUID.randomUUID();
        UUID tenantB = UUID.randomUUID();
        String first = createProposal(tenantA, "cliente-1");
        String second = createProposal(tenantA, "cliente-2");
        String third = createProposal(tenantA, "cliente-3");
        String other = createProposal(tenantB, "cliente-b");

        JsonNode pageA = list(tenantA, "");

        assertThat(ids(pageA)).containsExactly(third, second, first);
        assertThat(ids(list(tenantB, ""))).containsExactly(other);
        JsonNode item = pageA.path("items").get(0);
        assertThat(item.path("borrowerId").asText()).isEqualTo("cliente-3");
        assertThat(item.path("requestedAmount").decimalValue()).isEqualByComparingTo("5000.00");
        assertThat(item.path("termMonths").asInt()).isEqualTo(24);
        assertThat(item.path("createdAt").asText()).isNotBlank();
    }

    @Test
    void pagesAreConsecutiveWithoutRepeatingOrSkipping() throws Exception {
        UUID tenant = UUID.randomUUID();
        String first = createProposal(tenant, "c1");
        String second = createProposal(tenant, "c2");
        String third = createProposal(tenant, "c3");

        JsonNode page0 = list(tenant, "?page=0&size=2");
        JsonNode page1 = list(tenant, "?page=1&size=2");

        assertThat(ids(page0)).containsExactly(third, second);
        assertThat(page0.path("hasNext").asBoolean()).isTrue();
        assertThat(ids(page1)).containsExactly(first);
        assertThat(page1.path("hasNext").asBoolean()).isFalse();
    }

    @Test
    void aTenantWithoutProposalsGetsAnEmptyPage() throws Exception {
        JsonNode page = list(UUID.randomUUID(), "");

        assertThat(ids(page)).isEmpty();
        assertThat(page.path("hasNext").asBoolean()).isFalse();
    }
}
