# 0010 — Estratégia de testes em três níveis (unit, integração, contrato)

## Status
Aceita

## Contexto
Até a Milestone 11 todos os testes viviam juntos em `src/test` e `./gradlew test` rodava tudo de uma vez, sem distinção entre feedback rápido (sem Docker) e testes com Testcontainers. A única fronteira REST interna real — `funder-bot-service` chamando `POST /auctions/{proposalId}/bids` no `auction-service` — só era coberta por um stub `HttpServer` escrito à mão, que não impede consumidor e provedor de divergirem em silêncio. Nessa fronteira, o cliente do bot ignorava o status HTTP da resposta (`409`/`404` contavam como sucesso, sem log).

## Decisão
- Três tarefas Gradle por módulo, separadas por **padrão de nome de classe**: `test` (unitário — tudo que não casa com os padrões abaixo), `integrationTest` (`*IntegrationTest`, Testcontainers) e `contractTest` (`*ContractTest`, Pact). `make test` roda as três em ordem; o `ci.yml` as executa em passos separados. Divergência consciente do plano original (source sets `src/integrationTest`/`src/contractTest`): a convenção de nomes já existia nos 30 testes de integração e evita mover arquivos.
- **Pact só onde há contrato REST verificável dos dois lados**: `funder-bot-service` (consumidor) → `auction-service` (provedor), com os casos 200/409/404. O pact é um arquivo em `funder-bot-service/build/pacts`, sem Pact Broker; o `contractTest` do provedor depende do do consumidor. O consumidor só declara o que usa (status; o bot não lê o corpo da resposta).
- **Provedor verificado com repositório em memória** na porta `AuctionRepository` (controller real via MockMvc): o teste foca no formato HTTP; a persistência DynamoDB já tem teste de integração próprio.
- **Fora do Pact**: Jev/OpenRouter (terceiro, sem lado provedor verificável — coberto por stub e pelo `demo-smoke`) e `api-gateway` (proxy transparente, coberto por testes de integração). Eventos Kafka seguem compartilhando o módulo `event-schemas` (sem message pacts).
- Correção guiada pelo contrato: o `BidSubmitter` agora trata `5xx` como falha (conta no Circuit Breaker) e `4xx` como lance rejeitado (log com o status, sem contar como falha do circuito, para um leilão fechado não bloquear os demais bots).

## Consequências
- Feedback unitário rápido e sem Docker; contrato e integração isolados no log do CI.
- Um teste novo é classificado só pelo sufixo do nome — nomear errado o coloca no nível errado (o nível unitário não tem sufixo, então um teste com Testcontainers sem `IntegrationTest` no nome rodaria como unitário).
- Sem Pact Broker, o contrato só existe se o consumidor rodou no mesmo build; aceitável em monorepo, inviável se os serviços fossem repositórios separados (aí Broker/`can-i-deploy`).
- Versão usada: Pact JVM 4.7.5 (consumidor `junit5` com spec V3; provedor `spring6`), compatível com Java 21 e Spring Boot 3.5.
