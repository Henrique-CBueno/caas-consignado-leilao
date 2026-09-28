# C4 — Contexto do sistema

O CaaS Consignado Leilão é uma plataforma **simulada** de Credit-as-a-Service: bancos (tenants) originam propostas de crédito consignado, o sistema analisa o crédito e faz um leilão reverso entre financiadores até o desembolso.

```mermaid
C4Context
    title Contexto — CaaS Consignado Leilão

    Person(analista, "Analista do banco (tenant)", "Cria propostas e acompanha leilões e desembolsos")
    Person(apresentador, "Apresentador / avaliador", "Demonstra o fluxo, o trace e as métricas")

    System(caas, "CaaS Consignado Leilão", "Origina propostas, decide crédito, realiza leilão reverso de taxas, assina contrato e simula desembolso")

    System_Ext(jev, "Jev / OpenRouter Decisions API", "Decisão de crédito por modelo, com nível de confiança")
    System_Ext(cognito, "Cognito (emulado: cognito-local)", "Emite e valida tokens JWT")
    System_Ext(financiadores, "Financiadores", "Dão lances no leilão (simulados por bots neste projeto)")

    Rel(analista, caas, "Cria propostas e acompanha leilões", "HTTPS / WebSocket")
    Rel(apresentador, caas, "Observa traces e métricas", "Jaeger / Grafana")
    Rel(caas, jev, "Pede decisão de crédito", "HTTPS (Resilience4j)")
    Rel(caas, cognito, "Valida JWT", "JWKS")
    Rel(financiadores, caas, "Enviam lances", "HTTP")
```

Fronteiras honestas: o Jev só é chamado de verdade com `OPENROUTER_API_KEY` (perfil `jev`, ADR-0005); nos testes e no cluster local usa-se um adaptador simulado determinístico. Os financiadores são bots (ADR-0019).
