package com.caas.gateway.security

import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry
import io.github.resilience4j.ratelimiter.RateLimiterRegistry
import io.github.resilience4j.reactor.circuitbreaker.operator.CircuitBreakerOperator
import io.github.resilience4j.reactor.ratelimiter.operator.RateLimiterOperator
import org.springframework.beans.factory.annotation.Value
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.security.oauth2.jwt.NimbusReactiveJwtDecoder
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoder
import org.springframework.stereotype.Component
import reactor.core.publisher.Mono

// Circuit Breaker + Rate Limiter em volta da busca do JWKS do Cognito emulado:
// regra transversal do plano para toda chamada de rede de saída (ver ADR-016).
@Component
class ResilientJwtDecoder(
    @Value("\${app.cognito.jwk-set-uri}") jwkSetUri: String,
    circuitBreakerRegistry: CircuitBreakerRegistry,
    rateLimiterRegistry: RateLimiterRegistry,
) : ReactiveJwtDecoder {
    private val delegate = NimbusReactiveJwtDecoder.withJwkSetUri(jwkSetUri).build()
    private val circuitBreaker = circuitBreakerRegistry.circuitBreaker("cognito-jwks")
    private val rateLimiter = rateLimiterRegistry.rateLimiter("cognito-jwks")

    override fun decode(token: String): Mono<Jwt> =
        delegate.decode(token)
            .transformDeferred(CircuitBreakerOperator.of(circuitBreaker))
            .transformDeferred(RateLimiterOperator.of(rateLimiter))
}
