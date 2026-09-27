package com.caas.proposal.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import com.caas.proposal.application.ProposalRepository;
import com.caas.proposal.domain.Proposal;
import com.caas.proposal.domain.ProposalId;
import com.caas.proposal.domain.TenantId;
import com.caas.proposal.infrastructure.TenantContextHolder;
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
class ProposalRepositoryIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Autowired
    private ProposalRepository proposalRepository;

    @AfterEach
    void tearDown() {
        TenantContextHolder.clear();
    }

    private Proposal aProposal(TenantId tenantId) {
        return new Proposal(
            new ProposalId(UUID.randomUUID()),
            tenantId,
            "12345678900",
            new BigDecimal("5000.00"),
            24
        );
    }

    @Test
    void aTenantSessionCannotReadAnotherTenantsProposalById() {
        TenantId tenantA = new TenantId(UUID.randomUUID());
        TenantId tenantB = new TenantId(UUID.randomUUID());
        Proposal proposalOfA = aProposal(tenantA);

        TenantContextHolder.set(tenantA);
        proposalRepository.save(proposalOfA);

        TenantContextHolder.set(tenantB);
        Proposal leaked = proposalRepository.findById(proposalOfA.id());

        assertThat(leaked).isNull();
    }
}
