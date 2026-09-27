# 0003 — Transactional Outbox para publicar eventos de domínio no Kafka

## Status
Aceita

## Contexto
Persistir uma proposta no Postgres e publicar `ProposalCreated` no Kafka são duas operações contra sistemas diferentes. Sem coordenação, existe uma janela real de inconsistência: a transação do banco pode commitar e o publish no Kafka falhar (ou vice-versa), deixando uma proposta sem evento correspondente ou um evento órfão. Isso foi identificado como Risco #5 no plano de implementação.

## Decisão
Cada serviço que precisa publicar eventos de domínio junto com uma escrita transacional usa o padrão **transactional outbox**:
1. Uma tabela `outbox_events` (id, aggregate_type, aggregate_id, event_type, payload JSON, created_at, published_at nullable) é gravada na **mesma transação** que a escrita do agregado — no `proposal-service`, isso acontece porque `CreateProposalUseCase.execute()` é `@Transactional`, e tanto `ProposalRepository.save()` quanto `OutboxEventPublisher.publish()` usam propagação `REQUIRED` (padrão do Spring), então ambos entram na transação já aberta pelo use case em vez de abrirem a própria.
2. Um `OutboxRelay` (`@Scheduled`, delay curto — configurável via `app.outbox.relay-fixed-delay-ms`, default 500ms) varre periodicamente as linhas com `published_at IS NULL`, publica cada uma no tópico Kafka correspondente (usando o `aggregate_id` como partition key, garantindo ordenação por agregado) e marca `published_at` — usando o `.get()` bloqueante do `KafkaTemplate.send()` para só marcar como publicado depois de confirmação do broker (garante entrega "pelo menos uma vez": se o processo cair entre o send e o marcar-publicado, o evento é reenviado na próxima varredura — duplicação é possível, perda não).
3. A tabela `outbox_events` **não tem RLS**: é uma tabela puramente técnica, nunca consultada por um tenant, e o relay varre eventos de todos os tenants de uma vez.
4. O contrato do evento (`ProposalCreatedEvent`) vive em `libs/event-schemas`, um módulo Kotlin puro sem dependência de framework, compartilhado entre quem publica e quem consome (a partir da Milestone 3).

## Consequências
- Garantia: nunca existe uma proposta persistida sem o evento correspondente enfileirado para publicação (atomicidade da escrita), ao custo de latência entre o commit e a publicação de fato (bounded pelo `relay-fixed-delay-ms`).
- Entrega é "pelo menos uma vez", não exactly-once: consumidores de `ProposalCreated` (a partir da Milestone 3) precisam ser idempotentes em relação ao `proposalId`.
- O mapeamento `eventType → tópico Kafka` está hardcoded (`OutboxRelay.topicFor`) porque só existe um tipo de evento até agora — vira um registro de verdade (mapa injetável ou reflection sobre o tipo do payload) se/quando um segundo tipo aparecer, não antes (YAGNI).
- Testado via Testcontainers Kafka real (não mock): o teste de integração cria uma proposta pelo seam HTTP e consome a mensagem de um `KafkaConsumer` real no tópico `proposal.created`, provando que o relay funciona de ponta a ponta, não só que a linha da tabela foi gravada.
