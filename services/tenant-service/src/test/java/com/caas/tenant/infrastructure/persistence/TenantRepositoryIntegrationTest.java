package com.caas.tenant.infrastructure.persistence;

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
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@SpringBootTest
@Testcontainers
class TenantRepositoryIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Autowired
    private TenantRepository tenantRepository;

    @AfterEach
    void tearDown() {
        TenantContextHolder.clear();
    }

    @Test
    void aSavedTenantCanBeRetrievedById() {
        Tenant tenant = new Tenant(new TenantId(UUID.randomUUID()), "Banco Alfa Teste " + UUID.randomUUID());

        TenantContextHolder.set(tenant.id());
        tenantRepository.save(tenant);
        Tenant retrieved = tenantRepository.findById(tenant.id());

        assertThat(retrieved.name()).isEqualTo(tenant.name());
    }

    @Test
    void aTenantSessionCannotReadAnotherTenantsRowById() {
        Tenant tenantA = new Tenant(new TenantId(UUID.randomUUID()), "Banco Alfa Teste " + UUID.randomUUID());
        Tenant tenantB = new Tenant(new TenantId(UUID.randomUUID()), "Banco Beta Teste " + UUID.randomUUID());

        TenantContextHolder.set(tenantA.id());
        tenantRepository.save(tenantA);

        TenantContextHolder.set(tenantB.id());
        tenantRepository.save(tenantB);

        Tenant leaked = tenantRepository.findById(tenantA.id());

        assertThat(leaked).isNull();
    }
}
