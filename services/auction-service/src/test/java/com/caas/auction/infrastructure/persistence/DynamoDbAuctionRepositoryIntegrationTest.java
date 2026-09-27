package com.caas.auction.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import com.caas.auction.application.AuctionRepository;
import com.caas.auction.domain.Auction;
import com.caas.auction.domain.AuctionStatus;
import com.caas.auction.domain.Bid;
import com.caas.auction.domain.ProposalId;
import com.caas.auction.domain.TenantId;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.localstack.LocalStackContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

@SpringBootTest
@Testcontainers
class DynamoDbAuctionRepositoryIntegrationTest {

    @Container
    static LocalStackContainer localstack =
        new LocalStackContainer(DockerImageName.parse("localstack/localstack:3.8"))
            .withServices(LocalStackContainer.Service.DYNAMODB);

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add("app.dynamodb.endpoint",
            () -> localstack.getEndpointOverride(LocalStackContainer.Service.DYNAMODB).toString());
        registry.add("app.dynamodb.region", localstack::getRegion);
        registry.add("app.dynamodb.access-key", localstack::getAccessKey);
        registry.add("app.dynamodb.secret-key", localstack::getSecretKey);
    }

    @Autowired
    private AuctionRepository auctionRepository;

    @Test
    void aSavedAuctionCanBeRetrievedByProposalId() {
        Instant now = Instant.now().truncatedTo(ChronoUnit.MILLIS);
        Bid bid = new Bid("funder-alpha", new BigDecimal("1.99"), 24, now);
        Auction auction = new Auction(
            new ProposalId(UUID.randomUUID()),
            new TenantId(UUID.randomUUID()),
            AuctionStatus.OPEN,
            List.of("funder-alpha", "funder-beta"),
            now,
            now.plusSeconds(45),
            List.of(bid),
            null
        );

        auctionRepository.save(auction);
        Auction retrieved = auctionRepository.findByProposalId(auction.proposalId());

        assertThat(retrieved).isEqualTo(auction);
    }
}
