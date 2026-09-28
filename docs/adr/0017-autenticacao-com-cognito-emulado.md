# 0017 — Autenticação multi-tenant com Cognito emulado

## Status
Aceita

## Contexto
A borda precisa autenticar chamadas sem depender de um provedor de identidade real na máquina do desenvolvedor nem no CI.

## Decisão
O `cognito-local` (container) emula um User Pool. O `api-gateway` é *resource server* OAuth2: valida o JWT contra o JWKS configurado em `app.cognito.jwk-set-uri`, com Circuit Breaker e rate limiter próprios na busca das chaves (`cognito-jwks`), e exige autenticação em toda rota, exceto `/actuator/health` e `/actuator/prometheus` (ADR-0011). Os serviços internos não validam JWT: confiam no gateway e recebem o tenant em `X-Tenant-Id`. Os testes do gateway usam JWTs reais emitidos pelo emulador, nunca mockados.

## Consequências
- **Lacuna conhecida:** o JWT do emulador não carrega claim de tenant e o gateway não extrai nem injeta `X-Tenant-Id`; hoje o header vem do chamador (o `smoke-test` chama os serviços direto). O tenant no rate limit também é aproximado pela identidade (`sub`, ADR-0021).
- Em produção: atributo customizado (`custom:tenant_id`) mapeado no token, gateway removendo o header vindo do cliente e injetando o do claim, mais NetworkPolicies/mTLS para que só o gateway alcance os serviços internos.
