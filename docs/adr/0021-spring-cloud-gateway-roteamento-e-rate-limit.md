# 0021 — Spring Cloud Gateway: roteamento, resiliência e rate limit

## Status
Aceita

## Contexto
O sistema precisa de uma única borda pública que autentique, roteie e proteja os serviços internos.

## Decisão
Spring Cloud Gateway (reativo) roteia `/tenants/**` e `/proposals/**`, com URIs de destino por variável de ambiente (`APP_ROUTES_*_URI`). Cada rota tem Circuit Breaker próprio (`tenant-service-cb`, `proposal-service-cb`) com fallback `503` em `/fallback`. Um `PerIdentityRateLimitFilter` (`GlobalFilter` com precedência alta) aplica um limite por identidade — claim `sub` do JWT, 15 requisições por 10 s por padrão — com uma instância de rate limiter Resilience4j por identidade criada em runtime. Os demais serviços (leilão, desembolso etc.) não são roteados publicamente.

## Consequências
- Divergência do plano: o limite é por identidade, não por tenant, pois o JWT do emulador não tem claim de tenant (ADR-0017).
- Armadilhas reais de Reactor: `switchIfEmpty` sobre `Mono<Void>` reprocessa a requisição (nunca emite valor), e um `GlobalFilter` com `LOWEST_PRECEDENCE` roda depois do `NettyRoutingFilter`, então filtros que precisam embrulhar o roteamento exigem precedência alta.
