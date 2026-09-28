# 0016 — Persistência poliglota: Postgres e DynamoDB

## Status
Aceita

## Contexto
Agregados relacionais com isolamento por tenant pedem SQL e RLS; o leilão é acessado por chave, recebe escritas concorrentes de lances e precisa de atomicidade entre o estado e o evento a publicar.

## Decisão
Postgres (um por serviço, cada um dono do seu esquema com Flyway) para tenant, proposta, decisão de crédito, correlação de contrato e desembolso. DynamoDB para o leilão: tabela `auctions` (chave `proposal_id`, lances em `bids_json`) e tabela `outbox_events`; o agregado e o evento de outbox são gravados juntos com `TransactWriteItems` (equivalente ao `@Transactional` dos serviços SQL, ADR-0003). O RDS não é emulado pelo LocalStack Community, então o Postgres roda em containers (ADR-0002).

## Consequências
- Dois padrões de outbox coexistem (JPA e DynamoDB), cada um com seu relay.
- Não há transação entre serviços; a consistência entre contextos é por eventos e idempotência.
- Lances como JSON no item e escrita `Put` ingênua dão *last-write-wins* sob concorrência (ADR-0018).
