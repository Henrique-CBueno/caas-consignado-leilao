# 0022 — Resilience4j nas chamadas de rede de saída

## Status
Aceita

## Contexto
Chamadas síncronas a dependências externas ou internas falham; sem proteção, uma dependência lenta derruba quem a chama.

## Decisão
Resilience4j é aplicado onde existe chamada síncrona de saída: o `JevOpenRouterAdapter` (Circuit Breaker + rate limiter `jev-openrouter`), o `BidSubmitter` do bot (Circuit Breaker `auction-service-bid-submission`) e o gateway (por rota e no JWKS, ADR-0021). O chamador decide a degradação: a decisão de crédito vira `MANUAL_REVIEW` (ADR-0020); o bot deixa de participar. Semântica de status HTTP: `5xx` e erros de I/O contam como falha do circuito; `4xx` de negócio (leilão fechado ou inexistente) só é registrado, para um leilão encerrado não abrir o circuito dos demais bots. O estado dos circuitos é exportado como métrica (`resilience4j_circuitbreaker_state`) e aparece no dashboard do Grafana (ADR-0011).

## Consequências
- "Toda chamada de saída" do plano vale só onde há chamada síncrona; Kafka tem semântica de entrega própria.
- Sem retry, bulkhead nem timeouts uniformes; os parâmetros (janela 10, mínimo 5 chamadas, 50%, 5 s aberto) são os mesmos em todo lugar e não foram calibrados com carga.
