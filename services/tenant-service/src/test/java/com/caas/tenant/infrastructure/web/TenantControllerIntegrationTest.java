package com.caas.tenant.infrastructure.web;

import static org.assertj.core.api.Assertions.assertThat;

import com.caas.tenant.application.TenantRepository;
import com.caas.tenant.domain.Tenant;
import com.caas.tenant.domain.TenantId;
import com.caas.tenant.infrastructure.TenantContextHolder;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
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
class TenantControllerIntegrationTest {

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
    private TenantRepository tenantRepository;

    @Autowired
    private TestRestTemplate restTemplate;

    @AfterEach
    void tearDown() {
        TenantContextHolder.clear();
    }

    private void seed(Tenant tenant) {
        TenantContextHolder.set(tenant.id());
        tenantRepository.save(tenant);
        TenantContextHolder.clear();
    }

    private ResponseEntity<String> getTenant(UUID path, UUID callerTenantId) {
        HttpHeaders headers = new HttpHeaders();
        headers.set("X-Tenant-Id", callerTenantId.toString());
        return restTemplate.exchange(
            "http://localhost:" + port + "/tenants/" + path,
            HttpMethod.GET,
            new HttpEntity<Void>(headers),
            String.class
        );
    }

    @Test
    void anAuthenticatedTenantCanRetrieveItsOwnConfigViaTheApi() {
        Tenant tenantA = new Tenant(new TenantId(UUID.randomUUID()), "Banco Alfa Teste " + UUID.randomUUID());
        seed(tenantA);

        ResponseEntity<String> response = getTenant(tenantA.id().value(), tenantA.id().value());

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).contains(tenantA.name());
    }

    @Test
    void aTenantCannotRetrieveAnotherTenantsConfigViaTheApi() {
        Tenant tenantA = new Tenant(new TenantId(UUID.randomUUID()), "Banco Alfa Teste " + UUID.randomUUID());
        Tenant tenantB = new Tenant(new TenantId(UUID.randomUUID()), "Banco Beta Teste " + UUID.randomUUID());
        seed(tenantA);
        seed(tenantB);

        ResponseEntity<String> response = getTenant(tenantB.id().value(), tenantA.id().value());

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }
}
