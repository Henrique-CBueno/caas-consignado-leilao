package com.caas.credit.infrastructure.decision.jev;

import com.caas.credit.application.CreditDecisionPort;
import com.caas.credit.application.CreditDecisionRequest;
import com.caas.credit.domain.CreditDecisionResult;
import com.caas.credit.domain.Decision;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.ratelimiter.RateLimiter;
import io.github.resilience4j.ratelimiter.RateLimiterRegistry;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Map;
import java.util.function.Supplier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

// Decisions API real da OpenRouter (modelo typesafe/jev-1.13, primitivo Choice).
// Só ativa com app.credit-decision.provider=jev — MockDecisionAdapter é o padrão
// (ver ADR da Milestone 1/3): nunca depender de uma API externa paga em CI/local
// sem configuração explícita.
@Component
@ConditionalOnProperty(name = "app.credit-decision.provider", havingValue = "jev")
public class JevOpenRouterAdapter implements CreditDecisionPort {

    private static final String QUESTION_KEY = "credit_decision";

    private final String decisionsUrl;
    private final String apiKey;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient = HttpClient.newHttpClient();
    private final CircuitBreaker circuitBreaker;
    private final RateLimiter rateLimiter;

    public JevOpenRouterAdapter(
        @Value("${app.jev.decisions-url}") String decisionsUrl,
        @Value("${app.jev.api-key}") String apiKey,
        ObjectMapper objectMapper,
        CircuitBreakerRegistry circuitBreakerRegistry,
        RateLimiterRegistry rateLimiterRegistry
    ) {
        this.decisionsUrl = decisionsUrl;
        this.apiKey = apiKey;
        this.objectMapper = objectMapper;
        this.circuitBreaker = circuitBreakerRegistry.circuitBreaker("jev-openrouter");
        this.rateLimiter = rateLimiterRegistry.rateLimiter("jev-openrouter");
    }

    @Override
    public CreditDecisionResult decide(CreditDecisionRequest request) {
        Supplier<CreditDecisionResult> call = () -> callDecisionsApi(request);
        Supplier<CreditDecisionResult> resilient =
            CircuitBreaker.decorateSupplier(circuitBreaker, RateLimiter.decorateSupplier(rateLimiter, call));
        return resilient.get();
    }

    private CreditDecisionResult callDecisionsApi(CreditDecisionRequest request) {
        try {
            DecisionsRequest body = buildRequest(request);
            HttpRequest httpRequest = HttpRequest.newBuilder(URI.create(decisionsUrl))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + apiKey)
                .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(body)))
                .build();

            HttpResponse<String> httpResponse = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());
            DecisionsResponse response = objectMapper.readValue(httpResponse.body(), DecisionsResponse.class);
            ChoiceAnswer answer = response.answers().get(QUESTION_KEY);

            return new CreditDecisionResult(Decision.valueOf(answer.choice()), answer.confidence());
        } catch (Exception e) {
            throw new IllegalStateException("Falha ao chamar a Decisions API do Jev/OpenRouter", e);
        }
    }

    private DecisionsRequest buildRequest(CreditDecisionRequest request) {
        int simulatedCreditScore = Math.abs(request.borrowerId().hashCode()) % 1000;
        Map<String, Object> state = Map.of(
            "borrowerId", request.borrowerId(),
            "simulatedCreditScore", simulatedCreditScore,
            "requestedAmount", request.requestedAmount().toPlainString()
        );
        Map<String, String> criteria = Map.of(
            "APPROVE", "O tomador tem perfil de credito adequado para o valor solicitado.",
            "REJECT", "O tomador nao tem perfil de credito adequado para o valor solicitado.",
            "MANUAL_REVIEW", "Ha incerteza suficiente para exigir revisao humana antes de decidir."
        );
        ChoiceQuestion question = new ChoiceQuestion(
            "choice",
            "O credito deve ser aprovado, rejeitado, ou precisa de revisao manual?",
            criteria
        );
        return new DecisionsRequest("typesafe/jev-1.13", state, Map.of(QUESTION_KEY, question));
    }
}
