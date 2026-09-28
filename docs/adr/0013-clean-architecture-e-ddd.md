# 0013 — Clean Architecture e DDD em todos os serviços

## Status
Aceita

## Contexto
Sem uma regra de dependência explícita, regras de negócio acabam presas a JPA, Kafka ou Spring, e os testes só conseguem exercitá-las com infraestrutura completa.

## Decisão
Todo serviço segue três camadas: `domain` (records e value objects como `ProposalId`, `TenantId`, sem Spring), `application` (casos de uso e **portas**, como `AuctionRepository`, `CreditDecisionPort`, `OutboxEventPublisher`) e `infrastructure` (adaptadores: JPA, DynamoDB, Kafka, web, outbox). A dependência aponta sempre para dentro. Java 21 com records para DTOs e eventos; Lombok apenas nas entidades JPA (ADR-0004).

## Consequências
- Os testes escolhem seams nas portas e nas bordas (HTTP, Kafka) — ver ADR-0010.
- Mais tipos e mapeamentos do que um CRUD direto exigiria.
- Desvios conhecidos: `AuctionCloser` usa `@Scheduled` na camada `application`, e `ProposalController` valida `requestedAmount`/`termMonths` em vez de o domínio fazê-lo. São dívidas pequenas, registradas aqui em vez de escondidas.
