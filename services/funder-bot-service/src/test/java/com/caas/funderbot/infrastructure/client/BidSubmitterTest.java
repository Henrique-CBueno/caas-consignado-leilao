package com.caas.funderbot.infrastructure.client;

import static org.assertj.core.api.Assertions.assertThat;

import com.caas.observability.TraceContextStore;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpServer;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.micrometer.tracing.Tracer;
import io.micrometer.tracing.propagation.Propagator;
import java.math.BigDecimal;
import java.net.InetSocketAddress;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class BidSubmitterTest {

    private static final TraceContextStore NO_TRACING = new TraceContextStore(Tracer.NOOP, Propagator.NOOP);

    private HttpServer server;

    @AfterEach
    void stop() {
        if (server != null) {
            server.stop(0);
        }
    }

    private BidSubmitter submitterRespondingWith(int status, CircuitBreakerRegistry registry) throws Exception {
        server = HttpServer.create(new InetSocketAddress(0), 0);
        server.createContext("/", exchange -> {
            exchange.sendResponseHeaders(status, -1);
            exchange.close();
        });
        server.start();
        return new BidSubmitter("http://localhost:" + server.getAddress().getPort(), new ObjectMapper(), registry, NO_TRACING);
    }

    @Test
    void serverErrorCountsAsCircuitFailure() throws Exception {
        CircuitBreakerRegistry registry = CircuitBreakerRegistry.ofDefaults();
        BidSubmitter submitter = submitterRespondingWith(503, registry);

        submitter.submitBid(UUID.randomUUID(), "funder-1", new BigDecimal("2.2"), 24);

        var metrics = registry.circuitBreaker("auction-service-bid-submission").getMetrics();
        assertThat(metrics.getNumberOfFailedCalls()).isEqualTo(1);
        assertThat(metrics.getNumberOfSuccessfulCalls()).isZero();
    }
}
