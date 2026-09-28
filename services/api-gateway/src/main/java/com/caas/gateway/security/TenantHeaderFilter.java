package com.caas.gateway.security;

import java.util.Optional;
import java.util.UUID;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

// O tenant dos serviços internos vem do claim do JWT já validado, nunca de um header do cliente (ADR-0017).
@Component
public class TenantHeaderFilter implements GlobalFilter, Ordered {

    static final String TENANT_CLAIM = "custom:tenant_id";
    static final String TENANT_HEADER = "X-Tenant-Id";

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        return ReactiveSecurityContextHolder.getContext()
            .map(context -> Optional.ofNullable(
                ((JwtAuthenticationToken) context.getAuthentication()).getToken().getClaimAsString(TENANT_CLAIM)))
            .flatMap(tenant -> tenant
                .filter(TenantHeaderFilter::isUuid)
                .map(value -> chain.filter(exchange.mutate()
                    .request(request -> request.headers(headers -> headers.set(TENANT_HEADER, value)))
                    .build()))
                .orElseGet(() -> forbid(exchange)));
    }

    private static boolean isUuid(String value) {
        try {
            UUID.fromString(value);
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    // Autenticado, mas sem tenant válido: 403 (não 401) e nada é encaminhado ao serviço.
    private Mono<Void> forbid(ServerWebExchange exchange) {
        exchange.getResponse().setStatusCode(HttpStatus.FORBIDDEN);
        return exchange.getResponse().setComplete();
    }

    // Antes do rate limit (+10): requisição sem tenant válido não deve consumir cota.
    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE + 5;
    }
}
