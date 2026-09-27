package com.caas.gateway.security;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.ratelimiter.RateLimiter;
import io.github.resilience4j.ratelimiter.RateLimiterRegistry;
import io.github.resilience4j.reactor.circuitbreaker.operator.CircuitBreakerOperator;
import io.github.resilience4j.reactor.ratelimiter.operator.RateLimiterOperator;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.NimbusReactiveJwtDecoder;
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoder;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

// Circuit Breaker + Rate Limiter em volta da busca do JWKS do Cognito emulado:
// regra transversal do plano para toda chamada de rede de saída (ver ADR-016).
@Component
public class ResilientJwtDecoder implements ReactiveJwtDecoder {

    private final ReactiveJwtDecoder delegate;
    private final CircuitBreaker circuitBreaker;
    private final RateLimiter rateLimiter;

    public ResilientJwtDecoder(
        @Value("${app.cognito.jwk-set-uri}") String jwkSetUri,
        CircuitBreakerRegistry circuitBreakerRegistry,
        RateLimiterRegistry rateLimiterRegistry
    ) {
        this.delegate = NimbusReactiveJwtDecoder.withJwkSetUri(jwkSetUri).build();
        this.circuitBreaker = circuitBreakerRegistry.circuitBreaker("cognito-jwks");
        this.rateLimiter = rateLimiterRegistry.rateLimiter("cognito-jwks");
    }

    @Override
    public Mono<Jwt> decode(String token) {
        return delegate.decode(token)
            .transformDeferred(CircuitBreakerOperator.of(circuitBreaker))
            .transformDeferred(RateLimiterOperator.of(rateLimiter));
    }
}
