# 0017 — Autenticação multi-tenant com Cognito emulado

## Status
Aceita

## Contexto
A borda precisa autenticar chamadas sem depender de um provedor de identidade real na máquina do desenvolvedor nem no CI.

## Decisão
O `cognito-local` (container) emula um User Pool. O `api-gateway` é *resource server* OAuth2: valida o JWT contra o JWKS configurado em `app.cognito.jwk-set-uri`, com Circuit Breaker e rate limiter próprios na busca das chaves (`cognito-jwks`), e exige autenticação em toda rota, exceto `/actuator/health` e `/actuator/prometheus` (ADR-0011). Os serviços internos não validam JWT: confiam no gateway e recebem o tenant em `X-Tenant-Id`. Os testes do gateway usam JWTs reais emitidos pelo emulador, nunca mockados.

## Consequências
- O tenant vem do **ID token**: o `cognito-local` (e o Cognito real) emitem o atributo customizado `custom:tenant_id` no ID token, não no access token. Em produção o access token exigiria uma Lambda de pré-geração de token; não implementado.
- O gateway (`TenantHeaderFilter`) descarta qualquer `X-Tenant-Id` vindo do cliente e injeta o do claim; token válido sem claim, ou com claim que não é UUID, recebe **403** e nunca chega ao serviço. O rate limit é por tenant (ADR-0021).
- No cluster há um `cognito-local` com pool, client e um usuário por tenant de seed (`alfa@`, `beta@` e `gama@caas.local`, senha de demonstração), criados por um bootstrap idempotente no `make deploy-local`; o gateway aponta para o JWKS do pool criado (o id é gerado pelo emulador a cada criação).
- O acesso direto aos serviços que confiam em `X-Tenant-Id` é fechado por NetworkPolicy (Calico): só o gateway os alcança (ADR-0024). Sem isso, quem chegasse a um pod interno ainda escolheria o tenant.
- ~~Limitação: o WebSocket do `notification-gateway-service` segue público e sem autenticação.~~ Resolvida na Milestone 18 ([ADR-0030](0030-websocket-autenticado-por-tenant-e-cors-restrito.md)): o `CONNECT` exige o ID token e os tópicos são por tenant. Em produção: mTLS entre serviços e papéis além do tenant.
