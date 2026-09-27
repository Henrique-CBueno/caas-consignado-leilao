package com.caas.credit.infrastructure.decision.jev;

import static org.assertj.core.api.Assertions.assertThat;

import com.caas.credit.application.CreditDecisionRequest;
import com.caas.credit.domain.CreditDecisionResult;
import com.caas.credit.domain.Decision;
import com.caas.credit.domain.ProposalId;
import com.caas.credit.domain.TenantId;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpServer;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.ratelimiter.RateLimiterRegistry;
import java.math.BigDecimal;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class JevOpenRouterAdapterTest {

    private HttpServer stub;
    private final AtomicReference<String> receivedRequestBody = new AtomicReference<>();

    @BeforeEach
    void startStub() throws Exception {
        stub = HttpServer.create(new InetSocketAddress(0), 0);
        stub.createContext("/", exchange -> {
            receivedRequestBody.set(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            byte[] response = ("""
                {
                  "id": "gen-dec-test",
                  "model": "typesafe/jev-1.13",
                  "provider": "TypeSafe",
                  "answers": {
                    "credit_decision": {
                      "type": "choice",
                      "choice": "APPROVE",
                      "confidence": 0.82,
                      "probabilities": {"APPROVE": 0.82, "REJECT": 0.1, "MANUAL_REVIEW": 0.08}
                    }
                  }
                }
                """).getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, response.length);
            try (var os = exchange.getResponseBody()) {
                os.write(response);
            }
        });
        stub.start();
    }

    @AfterEach
    void stopStub() {
        stub.stop(0);
    }

    @Test
    void mapsTheDecisionsApiResponseToACreditDecisionResult() {
        JevOpenRouterAdapter adapter = new JevOpenRouterAdapter(
            "http://localhost:" + stub.getAddress().getPort(),
            "test-api-key",
            new ObjectMapper(),
            CircuitBreakerRegistry.ofDefaults(),
            RateLimiterRegistry.ofDefaults()
        );
        CreditDecisionRequest request = new CreditDecisionRequest(
            new ProposalId(UUID.randomUUID()),
            new TenantId(UUID.randomUUID()),
            "12345678900",
            new BigDecimal("5000.00")
        );

        CreditDecisionResult result = adapter.decide(request);

        assertThat(result.decision()).isEqualTo(Decision.APPROVE);
        assertThat(result.confidence()).isEqualTo(0.82);
        assertThat(receivedRequestBody.get())
            .contains("typesafe/jev-1.13")
            .contains("12345678900")
            .contains("\"type\":\"choice\"");
    }
}
