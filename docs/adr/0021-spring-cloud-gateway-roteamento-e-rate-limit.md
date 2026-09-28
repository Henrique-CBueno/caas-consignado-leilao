# 0021 — Spring Cloud Gateway: roteamento, resiliência e rate limit

## Status
Aceita

## Contexto
O sistema precisa de uma única borda pública que autentique, roteie e proteja os serviços internos.

## Decisão
Spring Cloud Gateway (reativo) roteia `/tenants/**`, `/proposals/**` e `/disbursements/**`, com URIs de destino por variável de ambiente (`APP_ROUTES_*_URI`). Cada rota tem Circuit Breaker próprio com fallback `503` em `/fallback`. Dois `GlobalFilter` de precedência alta, em ordem: o `TenantHeaderFilter` (ADR-0017) descarta o `X-Tenant-Id` do cliente, injeta o do claim `custom:tenant_id` e devolve 403 sem claim UUID válido; em seguida o `PerTenantRateLimitFilter` aplica o limite por tenant — 15 requisições por 10 s por padrão — com uma instância de rate limiter Resilience4j por tenant criada em runtime. Requisição sem tenant válido nem chega a consumir cota. Os demais serviços (leilão etc.) não são roteados publicamente.

## Consequências
- O limite é por tenant, como o plano previa (antes era por identidade `sub`, porque o token não carregava tenant): dois usuários do mesmo banco dividem a cota, e um banco não consome a de outro.
- Armadilhas reais de Reactor: `switchIfEmpty` sobre `Mono<Void>` reprocessa a requisição (nunca emite valor), e um `GlobalFilter` com `LOWEST_PRECEDENCE` roda depois do `NettyRoutingFilter`, então filtros que precisam embrulhar o roteamento exigem precedência alta.
