# 0025 — Escopo do shared kernel (`libs/`)

## Status
Aceita

## Contexto
Código compartilhado entre serviços vira acoplamento; sem uma regra, o módulo comum acaba abrigando lógica de domínio.

## Decisão
`libs/` contém apenas código **técnico**: `event-schemas` (records dos eventos Kafka, sem framework), `observability` (dependências, defaults e `TraceContextStore` da ADR-0011) e `openapi-testing` (helpers de teste de documentação, ADR-0010 e a spec de OpenAPI). Nunca lógica de domínio. O `domain-common` cogitado no plano não foi criado: nada justificou.

## Consequências
- Os eventos são um contrato por módulo: mudar um evento recompila os consumidores. Em produção, um schema registry com compatibilidade versionada.
- Cada serviço declara explicitamente a dependência, o que mantém o acoplamento visível no `build.gradle.kts`.
