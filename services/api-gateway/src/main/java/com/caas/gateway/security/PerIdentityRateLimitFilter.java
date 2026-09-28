package com.caas.gateway.security;

import io.github.resilience4j.ratelimiter.RateLimiter;
import io.github.resilience4j.ratelimiter.RateLimiterRegistry;
import io.github.resilience4j.ratelimiter.RequestNotPermitted;
import io.github.resilience4j.reactor.ratelimiter.operator.RateLimiterOperator;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

// Rate limit por identidade autenticada (claim "sub" do JWT já validado, nunca um
// header não confiável) — ver spec da Milestone 11 sobre por que não é por
// tenant de verdade ainda (o JWT do cognito-local não carrega esse claim hoje).
// Instâncias nomeadas dinamicamente no RateLimiterRegistry, uma por identidade,
// usando a config "per-identity" como base — mesmo padrão de registry nomeado já
// usado em jev-openrouter/cognito-jwks, aplicado a chaves que só existem em runtime.
//
// Sem fallback para "sem autenticação": SecurityConfig já exige
// anyExchange().authenticated() antes de qualquer request chegar aqui — não há
// rota pública neste gateway. (Um switchIfEmpty aqui seria, de qualquer forma,
// ambíguo: um Mono<Void> nunca emite valor, então não dá pra distinguir "vazio
// porque não achou identidade" de "vazio porque o trabalho terminou sem erro".)
@Component
public class PerIdentityRateLimitFilter implements GlobalFilter, Ordered {

    private static final String BASE_CONFIG_NAME = "per-identity";

    private final RateLimiterRegistry rateLimiterRegistry;

    public PerIdentityRateLimitFilter(RateLimiterRegistry rateLimiterRegistry) {
        this.rateLimiterRegistry = rateLimiterRegistry;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        return ReactiveSecurityContextHolder.getContext()
            .map(context -> (JwtAuthenticationToken) context.getAuthentication())
            .map(token -> token.getToken().getSubject())
            .flatMap(identity -> applyRateLimit(identity, exchange, chain));
    }

    private Mono<Void> applyRateLimit(String identity, ServerWebExchange exchange, GatewayFilterChain chain) {
        RateLimiter limiter = rateLimiterRegistry.rateLimiter(identity, BASE_CONFIG_NAME);
        return chain.filter(exchange)
            .transformDeferred(RateLimiterOperator.of(limiter))
            .onErrorResume(RequestNotPermitted.class, e -> {
                exchange.getResponse().setStatusCode(HttpStatus.TOO_MANY_REQUESTS);
                return exchange.getResponse().setComplete();
            });
    }

    // NettyRoutingFilter (e outros filtros de roteamento nativos do Gateway) usam
    // Ordered.LOWEST_PRECEDENCE — um filtro global nessa mesma prioridade pode
    // rodar depois do roteamento de verdade já ter acontecido. Prioridade alta
    // (valor baixo) garante que este filtro embrulhe a cadeia inteira, barrando
    // antes de qualquer coisa downstream (roteamento, Circuit Breaker da rota).
    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE + 10;
    }
}
