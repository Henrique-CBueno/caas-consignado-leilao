# 0007 — Correlação de dois eventos assíncronos no `contract-service`

## Status
Aceita

## Contexto
Gerar um contrato depende de dois fatos que vêm de bounded contexts diferentes, publicados de forma assíncrona e sem ordem garantida entre si: a revalidação de crédito pós-leilão aprovada (`CreditDecisionMade`, `stage=POST_AUCTION`, `decision=APPROVE`) e o fechamento do leilão com vencedor (`AuctionClosed`, `status=CLOSED_WITH_WINNER`). É o primeiro ponto do projeto em que um serviço precisa esperar por mais de um evento de entrada antes de agir — os outros consumidores (auction-service, credit-analysis-service pré-Milestone 8) sempre reagiam a um único evento por vez.

## Decisão
O `contract-service` mantém uma tabela de correlação (`contract_correlations`, chave primária `proposal_id`) com colunas nuláveis para cada fato esperado. Cada um dos dois listeners (`CreditDecisionMadeListener`, `AuctionClosedListener`) grava o que já sabe naquela linha (criando-a se ainda não existir) e, só depois de gravar, verifica se a linha está completa; se estiver e o contrato ainda não tiver sido gerado, gera o `Contract` e publica `ContractSigned` na mesma transação que marca a correlação como concluída (evita publicar duas vezes se um dos eventos for reprocessado).

A correlação mora no `contract-service`, não em um orquestrador central: é o bounded context que naturalmente precisa dos dois fatos para existir (um contrato não faz sentido sem crédito aprovado nem sem vencedor de leilão) — coreografia via Kafka, não orquestração.

**Corrida de escrita concorrente**: os dois listeners podem processar a mesma proposta quase ao mesmo tempo (ex.: `AuctionClosed` e a revalidação de crédito chegando em sequência rápida). Um `SELECT ... FOR UPDATE` não protege a primeira escrita — se a linha ainda não existe, não há o que travar, e os dois listeners podem tentar inserir a mesma chave primária simultaneamente (violação de constraint única, encontrada empiricamente durante o desenvolvimento desta milestone). A solução usada é `pg_advisory_xact_lock`, tomado por uma chave derivada do `proposalId` antes de ler-ou-criar a correlação — serializa as duas transações concorrentes para a mesma proposta mesmo quando nenhuma linha existe ainda, e é liberado automaticamente ao fim da transação.

## Consequências
- Sem timeout/retry se um dos dois eventos nunca chegar: a correlação fica pendente indefinidamente (registrado como simplificação de demo na spec da Milestone 8, não implementado).
- O padrão (tabela de correlação nulável + advisory lock + outbox transacional) é reutilizável se um futuro consumidor precisar esperar por mais de dois eventos — a mesma ideia escala trocando os campos nuláveis, não a mecânica de lock.
- Alternativa descartada: orquestração central (um serviço/processo que chama explicitamente credit-analysis-service e auction-service e decide quando gerar o contrato) — abandonaria o estilo de coreografia via eventos já usado em todo o resto do projeto só para este caso, sem necessidade real.
