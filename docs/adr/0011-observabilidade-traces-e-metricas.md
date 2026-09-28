# 0011 — Observabilidade: trace único (Jaeger via OTLP) e métricas (Prometheus/Grafana)

## Status
Aceita

## Contexto
Uma proposta atravessa `proposal → credit-analysis → auction → funder-bot → auction → credit-analysis → contract → disbursement`, quase toda por Kafka. Sem tracing não havia como responder "por onde essa proposta passou". O padrão de transactional outbox (ADR-0003) agrava o problema: o evento é gravado dentro do caso de uso e publicado depois por um `@Scheduled`, em outra thread e sem contexto — cada publicação abriria um trace novo.

## Decisão
- **Módulo técnico `libs/observability`**: dependências (Actuator, registro Prometheus, ponte Micrometer Tracing → OpenTelemetry, exportador OTLP) e `observability-defaults.yml` (importado por cada serviço): endpoints `health`/`prometheus`, observação habilitada em produtores e consumidores Kafka, amostragem 100%, endpoint OTLP por variável de ambiente. Nenhum SDK direto nos serviços, sem agente Java.
- **Transporte**: OTLP/HTTP direto para o Jaeger all-in-one (storage em memória), sem OpenTelemetry Collector. Em produção o Collector entraria para buffer, amostragem e roteamento.
- **Propagação pelo outbox**: a tabela/item de outbox ganha o `traceparent` (W3C) de quem gravou o evento (`TraceContextStore.capture()`); o relay restaura esse contexto como pai (`inSpan`) antes de enviar ao Kafka, e a observação do `KafkaTemplate` injeta o header. Postgres: migration nova nos 3 serviços; DynamoDB (`auction-service`): atributo novo, sem migration. Eventos antigos sem o campo publicam normalmente, sem pai.
- **`AuctionClosed` (scheduler, sem requisição)**: o item do leilão guarda o `traceparent` de quando foi aberto (preservado a cada reescrita) e o `AuctionCloser` o restaura ao fechar — o fechamento continua o trace da proposta.
- **`funder-bot-service`**: o contexto do listener é capturado e restaurado na thread agendada do lance; como `java.net.http.HttpClient` não é instrumentado, o `traceparent` é injetado explicitamente no `POST /auctions/{id}/bids`.
- **Métricas**: Prometheus em manifests simples (descoberta de pods por anotação `prometheus.io/scrape`, sem operator) e Grafana com datasources e um dashboard provisionados por ConfigMap. Sem métricas de negócio novas: HTTP, JVM, Kafka e estado dos Circuit Breakers do Resilience4j.
- **Web em `credit-analysis-service` e `funder-bot-service`**: `starter-web` só para expor o Actuator; o gateway libera sem JWT apenas `/actuator/health` e `/actuator/prometheus`.
- **Minikube**: perfil dedicado (`caas`, 8 GiB / 6 CPUs, sobrescrevível por `MINIKUBE_PROFILE`), porque o perfil padrão `minikube` pode ser de outro projeto e a memória de um perfil existente não é redimensionável. O stack de observabilidade entra só no deploy completo, não no `SLIM=1` do CI (ADR-0009).
- **Prova**: `make smoke-test` (deploy completo) manda um `traceparent` conhecido no `POST /proposals` e exige, no Jaeger, um único trace com spans dos 6 serviços da cadeia; exige alvos `up` no Prometheus e o dashboard "CaaS Overview" no Grafana.

## Consequências
- Nos testes, `@SpringBootTest` desliga o tracing por padrão (`management.tracing.enabled=false`, propagador vira noop): os testes de propagação usam `@AutoConfigureObservability`.
- Jaeger e Prometheus não persistem dados (retenção de 2 h no Prometheus, traces em memória); sem alertas/Alertmanager, sem logs centralizados (Loki).
- Amostragem 100% é aceitável só em demonstração.
- O `contract-service` correlaciona dois eventos: o `ContractSigned` continua o trace do evento que chegou por último; os dois pertencem ao mesmo trace da proposta.
- No modo slim do CI os serviços ainda tentam exportar para `jaeger:4318` (ausente) e logam falhas de export periodicamente — ruído aceito, sem efeito funcional.
