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

// Rate limit por tenant (claim custom:tenant_id do JWT já validado, nunca um header
// não confiável — ADR-0017/0021). O TenantHeaderFilter roda antes e barra tokens sem
// tenant válido, então aqui o claim sempre existe.
// Instâncias nomeadas dinamicamente no RateLimiterRegistry, uma por tenant,
// usando a config "per-tenant" como base — mesmo padrão de registry nomeado já
// usado em jev-openrouter/cognito-jwks, aplicado a chaves que só existem em runtime.
//
// Sem fallback para "sem autenticação": SecurityConfig já exige
// anyExchange().authenticated() antes de qualquer request chegar aqui — não há
// rota pública neste gateway. (Um switchIfEmpty aqui seria, de qualquer forma,
// ambíguo: um Mono<Void> nunca emite valor, então não dá pra distinguir "vazio
// porque não achou identidade" de "vazio porque o trabalho terminou sem erro".)
@Component
public class PerTenantRateLimitFilter implements GlobalFilter, Ordered {

    private static final String BASE_CONFIG_NAME = "per-tenant";

    private final RateLimiterRegistry rateLimiterRegistry;

    public PerTenantRateLimitFilter(RateLimiterRegistry rateLimiterRegistry) {
        this.rateLimiterRegistry = rateLimiterRegistry;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        return ReactiveSecurityContextHolder.getContext()
            .map(context -> (JwtAuthenticationToken) context.getAuthentication())
            // /admin/** não tem tenant (Milestone 17): a identidade administrativa tem a própria cota.
            .map(token -> java.util.Objects.requireNonNullElse(
                token.getToken().getClaimAsString(TenantHeaderFilter.TENANT_CLAIM), "admin"))
            .flatMap(tenant -> applyRateLimit(tenant, exchange, chain));
    }

    private Mono<Void> applyRateLimit(String tenant, ServerWebExchange exchange, GatewayFilterChain chain) {
        RateLimiter limiter = rateLimiterRegistry.rateLimiter(tenant, BASE_CONFIG_NAME);
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
