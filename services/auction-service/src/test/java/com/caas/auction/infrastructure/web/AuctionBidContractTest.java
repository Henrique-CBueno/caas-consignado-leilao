package com.caas.auction.infrastructure.web;

import au.com.dius.pact.provider.junit5.PactVerificationContext;
import au.com.dius.pact.provider.junitsupport.Provider;
import au.com.dius.pact.provider.junitsupport.State;
import au.com.dius.pact.provider.junitsupport.loader.PactFolder;
import au.com.dius.pact.provider.spring.spring6.Spring6MockMvcTestTarget;
import au.com.dius.pact.provider.spring.spring6.PactVerificationSpring6Provider;
import com.caas.auction.domain.Auction;
import com.caas.auction.domain.AuctionStatus;
import com.caas.auction.domain.ProposalId;
import com.caas.auction.domain.TenantId;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.TestTemplate;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(AuctionController.class)
@Import(AuctionBidContractTest.Config.class)
@Provider("auction-service")
@PactFolder("../funder-bot-service/build/pacts")
class AuctionBidContractTest {

    @TestConfiguration
    static class Config {
        @Bean
        InMemoryAuctionRepository auctionRepository() {
            return new InMemoryAuctionRepository();
        }
    }

    @Autowired
    MockMvc mockMvc;

    @Autowired
    InMemoryAuctionRepository repository;

    @BeforeEach
    void setUp(PactVerificationContext context) {
        repository.clear();
        context.setTarget(new Spring6MockMvcTestTarget(mockMvc));
    }

    @TestTemplate
    @ExtendWith(PactVerificationSpring6Provider.class)
    void verifyBotContract(PactVerificationContext context) {
        context.verifyInteraction();
    }

    @State("leilão aberto para lances")
    void openAuction() {
        repository.save(auction("11111111-1111-1111-1111-111111111111", AuctionStatus.OPEN, Instant.now().plusSeconds(60)));
    }

    @State("leilão fechado")
    void closedAuction() {
        repository.save(auction("22222222-2222-2222-2222-222222222222", AuctionStatus.CLOSED_NO_WINNER, Instant.now().minusSeconds(60)));
    }

    @State("leilão inexistente")
    void missingAuction() {
        // nenhum leilão salvo
    }

    private Auction auction(String proposalId, AuctionStatus status, Instant expiresAt) {
        return new Auction(
            new ProposalId(UUID.fromString(proposalId)), new TenantId(UUID.randomUUID()), status,
            List.of("funder-1"), Instant.now().minusSeconds(120), expiresAt, List.of(), null
        );
    }
}
