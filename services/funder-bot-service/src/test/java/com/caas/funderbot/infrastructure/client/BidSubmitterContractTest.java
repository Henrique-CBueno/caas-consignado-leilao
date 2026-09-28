package com.caas.funderbot.infrastructure.client;

import static org.assertj.core.api.Assertions.assertThat;

import au.com.dius.pact.consumer.MockServer;
import au.com.dius.pact.consumer.dsl.PactDslJsonBody;
import au.com.dius.pact.consumer.dsl.PactDslWithProvider;
import au.com.dius.pact.consumer.junit5.PactConsumerTestExt;
import au.com.dius.pact.consumer.junit5.PactTestFor;
import au.com.dius.pact.core.model.PactSpecVersion;
import au.com.dius.pact.core.model.RequestResponsePact;
import au.com.dius.pact.core.model.annotations.Pact;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.caas.observability.TraceContextStore;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.micrometer.tracing.Tracer;
import io.micrometer.tracing.propagation.Propagator;
import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.slf4j.LoggerFactory;

@ExtendWith(PactConsumerTestExt.class)
@PactTestFor(providerName = "auction-service", pactVersion = PactSpecVersion.V3)
class BidSubmitterContractTest {

    private static final TraceContextStore NO_TRACING = new TraceContextStore(Tracer.NOOP, Propagator.NOOP);

    static final UUID OPEN_AUCTION = UUID.fromString("11111111-1111-1111-1111-111111111111");

    static final UUID CLOSED_AUCTION = UUID.fromString("22222222-2222-2222-2222-222222222222");

    static final UUID MISSING_AUCTION = UUID.fromString("33333333-3333-3333-3333-333333333333");

    private final CircuitBreakerRegistry registry = CircuitBreakerRegistry.ofDefaults();
    private final ListAppender<ILoggingEvent> logs = new ListAppender<>();
    private final Logger submitterLogger = (Logger) LoggerFactory.getLogger(BidSubmitter.class);

    @BeforeEach
    void captureLogs() {
        logs.start();
        submitterLogger.addAppender(logs);
    }

    @AfterEach
    void releaseLogs() {
        submitterLogger.detachAppender(logs);
    }

    private boolean logged(String... fragments) {
        return logs.list.stream().map(ILoggingEvent::getFormattedMessage)
            .anyMatch(m -> java.util.Arrays.stream(fragments).allMatch(m::contains));
    }

    private BidSubmitter submitterFor(MockServer server) {
        return new BidSubmitter(server.getUrl(), new ObjectMapper(), registry, NO_TRACING);
    }

    private CircuitBreaker breaker() {
        return registry.circuitBreaker("auction-service-bid-submission");
    }

    @Pact(consumer = "funder-bot-service")
    RequestResponsePact bidAccepted(PactDslWithProvider builder) {
        return builder
            .given("leilão aberto para lances")
            .uponReceiving("um lance de um bot em leilão aberto")
            .method("POST")
            .path("/auctions/" + OPEN_AUCTION + "/bids")
            .headers(Map.of("Content-Type", "application/json"))
            .body(new PactDslJsonBody()
                .stringType("funderId", "funder-1")
                .decimalType("rate", 2.2)
                .integerType("termMonths", 24))
            .willRespondWith()
            .status(200)
            .toPact();
    }

    @Test
    @PactTestFor(pactMethod = "bidAccepted")
    void acceptedBidIsSentInTheAgreedShapeAndCountsAsSuccess(MockServer server) {
        submitterFor(server).submitBid(OPEN_AUCTION, "funder-1", new BigDecimal("2.2"), 24);

        assertThat(breaker().getMetrics().getNumberOfSuccessfulCalls()).isEqualTo(1);
        assertThat(breaker().getMetrics().getNumberOfFailedCalls()).isZero();
    }

    @Pact(consumer = "funder-bot-service")
    RequestResponsePact auctionClosed(PactDslWithProvider builder) {
        return builder
            .given("leilão fechado")
            .uponReceiving("um lance de um bot em leilão já fechado")
            .method("POST")
            .path("/auctions/" + CLOSED_AUCTION + "/bids")
            .headers(Map.of("Content-Type", "application/json"))
            .body(new PactDslJsonBody()
                .stringType("funderId", "funder-1")
                .decimalType("rate", 2.2)
                .integerType("termMonths", 24))
            .willRespondWith()
            .status(409)
            .toPact();
    }

    @Test
    @PactTestFor(pactMethod = "auctionClosed")
    void bidOnAClosedAuctionIsLoggedAsRejectedWithoutCountingAsCircuitFailure(MockServer server) {
        submitterFor(server).submitBid(CLOSED_AUCTION, "funder-1", new BigDecimal("2.2"), 24);

        assertThat(logged("funder-1", CLOSED_AUCTION.toString(), "409")).isTrue();
        assertThat(breaker().getMetrics().getNumberOfFailedCalls()).isZero();
    }

    @Pact(consumer = "funder-bot-service")
    RequestResponsePact auctionNotFound(PactDslWithProvider builder) {
        return builder
            .given("leilão inexistente")
            .uponReceiving("um lance de um bot em leilão que não existe")
            .method("POST")
            .path("/auctions/" + MISSING_AUCTION + "/bids")
            .headers(Map.of("Content-Type", "application/json"))
            .body(new PactDslJsonBody()
                .stringType("funderId", "funder-1")
                .decimalType("rate", 2.2)
                .integerType("termMonths", 24))
            .willRespondWith()
            .status(404)
            .toPact();
    }

    @Test
    @PactTestFor(pactMethod = "auctionNotFound")
    void bidOnAnUnknownAuctionIsLoggedAsRejectedWithoutCountingAsCircuitFailure(MockServer server) {
        submitterFor(server).submitBid(MISSING_AUCTION, "funder-1", new BigDecimal("2.2"), 24);

        assertThat(logged("funder-1", MISSING_AUCTION.toString(), "404")).isTrue();
        assertThat(breaker().getMetrics().getNumberOfFailedCalls()).isZero();
    }
}
