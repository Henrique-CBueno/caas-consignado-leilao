package com.caas.credit.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import com.caas.credit.application.CreditDecisionRepository;
import com.caas.credit.domain.CreditDecision;
import com.caas.credit.domain.CreditDecisionId;
import com.caas.credit.domain.Decision;
import com.caas.credit.domain.ProposalId;
import com.caas.credit.domain.Stage;
import com.caas.credit.domain.TenantId;
import com.caas.credit.infrastructure.TenantContextHolder;
import java.math.BigDecimal;
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
class CreditDecisionRepositoryIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Autowired
    private CreditDecisionRepository creditDecisionRepository;

    @AfterEach
    void tearDown() {
        TenantContextHolder.clear();
    }

    @Test
    void aTenantSessionCannotReadAnotherTenantsCreditDecisionById() {
        TenantId tenantA = new TenantId(UUID.randomUUID());
        TenantId tenantB = new TenantId(UUID.randomUUID());
        CreditDecision decisionOfA = new CreditDecision(
            new CreditDecisionId(UUID.randomUUID()),
            new ProposalId(UUID.randomUUID()),
            tenantA,
            "12345678900",
            new BigDecimal("5000.00"),
            Decision.APPROVE,
            0.9,
            Stage.PRE_AUCTION
        );

        TenantContextHolder.set(tenantA);
        creditDecisionRepository.save(decisionOfA);

        TenantContextHolder.set(tenantB);
        CreditDecision leaked = creditDecisionRepository.findById(decisionOfA.id());

        assertThat(leaked).isNull();
    }
}
