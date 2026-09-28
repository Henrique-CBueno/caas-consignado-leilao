# 0012 — Decomposição em bounded contexts e microsserviços

## Status
Aceita

## Contexto
O fluxo de negócio é uma cadeia: proposta → decisão de crédito → leilão reverso → revalidação → contrato → desembolso. Cada etapa tem regras, dados e ritmo de mudança próprios, e o projeto existe para demonstrar julgamento de arquitetura distribuída.

## Decisão
Um serviço por bounded context, cada um dono do seu dado:

| Contexto | Serviço | Dado |
|---|---|---|
| Tenants (seed) | `tenant-service` | Postgres |
| Borda pública | `api-gateway` | — |
| Originação | `proposal-service` | Postgres |
| Decisão de crédito | `credit-analysis-service` | Postgres |
| Leilão | `auction-service` | DynamoDB |
| Financiadores simulados | `funder-bot-service` | — |
| Tempo real | `notification-gateway-service` | — |
| Contrato | `contract-service` | Postgres |
| Desembolso | `disbursement-service` | Postgres |

A integração é assíncrona por eventos Kafka (`proposal.created`, `credit.decision.made`, `auction.opened`, `auction.bid.placed`, `auction.closed`, `contract.signed`) com schemas em `libs/event-schemas` (ADR-0025). O caminho síncrono existe só onde a interação é comando/resposta: gateway → serviços e bot → leilão (`POST /auctions/{id}/bids`).

## Consequências
- Consistência eventual entre contextos; a correlação de eventos que chegam em ordem arbitrária é tratada caso a caso (ADR-0007).
- Nove serviços numa máquina de desenvolvimento têm custo real de memória e de operação (ADR-0024).
- Os eventos são um contrato compartilhado por módulo Gradle; em produção seria um schema registry com versionamento.
