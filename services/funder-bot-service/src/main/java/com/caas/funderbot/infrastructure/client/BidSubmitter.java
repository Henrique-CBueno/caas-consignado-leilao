package com.caas.funderbot.infrastructure.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import java.math.BigDecimal;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

// Sem retry: se o circuito estiver aberto ou a chamada falhar, o bot simplesmente
// não participa daquele leilão (simplificação registrada na spec da Milestone 7).
@Component
public class BidSubmitter {

    private static final Logger log = LoggerFactory.getLogger(BidSubmitter.class);

    private final String baseUrl;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient = HttpClient.newHttpClient();
    private final CircuitBreaker circuitBreaker;

    public BidSubmitter(
        @Value("${app.auction-service.base-url}") String baseUrl,
        ObjectMapper objectMapper,
        CircuitBreakerRegistry circuitBreakerRegistry
    ) {
        this.baseUrl = baseUrl;
        this.objectMapper = objectMapper;
        this.circuitBreaker = circuitBreakerRegistry.circuitBreaker("auction-service-bid-submission");
    }

    public void submitBid(UUID proposalId, String funderId, BigDecimal rate, int termMonths) {
        try {
            circuitBreaker.executeCallable(() -> doSubmit(proposalId, funderId, rate, termMonths));
        } catch (Exception e) {
            log.warn("Falha ao submeter lance de {} para o leilão {}: {}", funderId, proposalId, e.getMessage());
        }
    }

    private Void doSubmit(UUID proposalId, String funderId, BigDecimal rate, int termMonths) throws Exception {
        String body = objectMapper.writeValueAsString(new BidPayload(funderId, rate, termMonths));
        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(baseUrl + "/auctions/" + proposalId + "/bids"))
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(body))
            .build();
        httpClient.send(request, HttpResponse.BodyHandlers.discarding());
        return null;
    }
}
