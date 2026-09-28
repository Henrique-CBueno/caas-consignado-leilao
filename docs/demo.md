# Roteiro de demonstração

Roteiro para apresentar o sistema ao vivo (~15 min). **Todos os comandos abaixo foram executados de verdade** num cluster real (perfil minikube `caas`, 8 GiB), com as saídas observadas descritas em cada passo. Os tempos são aproximados.

Pré-requisitos: Docker, `minikube`, `kubectl`, `helm`, JDK 21 e Node 22; cerca de 8 GiB livres. Em todo o roteiro, `IP` é o endereço do cluster:

```
IP=$(minikube -p caas ip)
```

## 1. Subir o cluster (~10 min na primeira vez)

```
make deploy-local
```

Compila os 9 serviços e o dashboard, sobe o minikube (perfil `caas`), instala Kafka + Zookeeper e Vault por Helm e aplica Postgres (um StatefulSet por serviço), LocalStack, os serviços e o stack de observabilidade. Termina quando todos os pods estão `Ready` (22 pods `1/1`).

## 2. Fluxo feliz completo, com prova de observabilidade (~1 min)

```
make smoke-test
```

Cria uma proposta e espera o desembolso. Saída esperada (resumida):

```
proposal created: {"id":"…","borrowerId":"59","requestedAmount":5000.00,"termMonths":24,"status":"PENDING_CREDIT_ANALYSIS"}
DISBURSEMENT FOUND:
{"proposalId":"…","funderId":"funder-2","amount":5000.00,"status":"DISBURSED",…}
TRACE OK (…): auction-service contract-service credit-analysis-service disbursement-service funder-bot-service notification-gateway-service proposal-service
PROMETHEUS OK: 9 alvos up
GRAFANA OK: dashboard 'CaaS Overview' provisionado
```

O que isso prova: a proposta atravessou todos os serviços por eventos, o leilão escolheu um vencedor (`funder-2`, ver [ADR-0018](adr/0018-mecanismo-de-leilao-e-desempate.md)), o contrato foi assinado e o desembolso ficou consultável; e **um único trace** cobre a cadeia inteira.

## 3. Leilão ao vivo no dashboard (~2 min)

Os bots dão lance de 0,5 a 3 s depois de o leilão abrir, rápido demais para abrir a tela. Para a demonstração, atrase os bots e exponha o `proposal-service`:

```
kubectl set env deploy/funder-bot-service -n caas APP_FUNDER_BOT_MIN_DELAY_MS=10000 APP_FUNDER_BOT_MAX_DELAY_MS=25000
kubectl rollout status deploy/funder-bot-service -n caas
kubectl port-forward -n caas svc/proposal-service 8081:8080
```

Em outro terminal, abra `http://$IP:30090` (dashboard Angular) e crie uma proposta:

```
curl -s -X POST localhost:8081/proposals \
  -H 'Content-Type: application/json' \
  -H 'X-Tenant-Id: 11111111-1111-1111-1111-111111111111' \
  -d '{"borrowerId":"59","requestedAmount":5000.00,"termMonths":24}'
```

Copie o `id` da resposta para o campo "ID da proposta" e clique em **Acompanhar** (o indicador vira "conectado"). Em 10–25 s os três lances aparecem ao vivo; a janela do leilão é de 45 s e, ao fechar, o banner mostra o vencedor (menor taxa; empate por prazo e depois por horário). Exemplo observado: `Leilão fechado: CLOSED_WITH_WINNER — vencedor funder-2 a 1.92%`.

![Leilão ao vivo](img/dashboard-leilao.png)

Injeção manual de lance (o mesmo endpoint dos bots, [ADR-0019](adr/0019-financiadores-simulados-como-servico.md)), enquanto o leilão está aberto:

```
kubectl port-forward -n caas svc/auction-service 8082:8080
curl -s -X POST localhost:8082/auctions/<proposalId>/bids -H 'Content-Type: application/json' \
  -d '{"funderId":"funder-1","rate":1.5,"termMonths":24}'
```

Saída observada: `HTTP 200` com o leilão atualizado; ao fechar (`GET /auctions/<proposalId>`), `CLOSED_WITH_WINNER` com vencedor `funder-1` — o lance manual de 1,5% venceu os bots (~2,0–2,6%).

Ao terminar, restaure os bots:

```
kubectl set env deploy/funder-bot-service -n caas APP_FUNDER_BOT_MIN_DELAY_MS- APP_FUNDER_BOT_MAX_DELAY_MS-
```

## 4. O trace no Jaeger (~2 min)

Abra `http://$IP:30686`, escolha o serviço `proposal-service` e abra o trace de `http post /proposals`. Ele mostra uma árvore conectada de **7 serviços e 39 spans (profundidade 17)** — do `POST` inicial, passando pelo outbox (`outbox-relay ProposalCreated`), pelos consumidores Kafka, pelos lances dos bots por HTTP e pelo fechamento do leilão, até o desembolso. Como a raiz é a requisição, os saltos assíncronos ficam sob ela graças ao `traceparent` guardado no outbox ([ADR-0011](adr/0011-observabilidade-traces-e-metricas.md)).

![Trace único no Jaeger](img/jaeger-trace.png)

## 5. Métricas no Grafana (~1 min)

Abra `http://$IP:30300/d/caas-overview` (acesso anônimo somente leitura): taxa e latência p95 de HTTP, heap por serviço, mensagens Kafka publicadas e consumidas, e o estado dos Circuit Breakers.

![Dashboard do Grafana](img/grafana-dashboard.png)

## 6. Degradação graciosa: Jev indisponível (~2 min)

Aponte a decisão de crédito para o provedor Jev com uma URL inalcançável (simula a queda do provedor):

```
kubectl set env deploy/credit-analysis-service -n caas APP_CREDIT_DECISION_PROVIDER=jev APP_JEV_DECISIONS_URL=http://localhost:1/decisions
kubectl rollout status deploy/credit-analysis-service -n caas
```

Crie uma proposta (comando do passo 3) e leia a decisão publicada no Kafka:

```
kubectl exec -n caas kafka-broker-0 -- /opt/bitnami/kafka/bin/kafka-console-consumer.sh \
  --bootstrap-server localhost:9092 --topic credit.decision.made --from-beginning --timeout-ms 8000 | grep <proposalId>
```

Saída observada: `{"proposalId":"…","decision":"MANUAL_REVIEW","confidence":0.0,"stage":"PRE_AUCTION",…}` — o sistema **não quebra**: a falha do provedor vira revisão manual e nenhum leilão é aberto ([ADR-0020](adr/0020-trava-de-confianca-e-degradacao-do-credito.md)). Volte ao normal:

```
kubectl set env deploy/credit-analysis-service -n caas APP_CREDIT_DECISION_PROVIDER- APP_JEV_DECISIONS_URL-
```

A chamada real ao Jev (com `OPENROUTER_API_KEY`) é o `demo-smoke` do CI e não faz parte deste roteiro ([ADR-0005](adr/0005-integracao-jev-openrouter-e-vault.md)).

## 7. Encerrar

```
make k8s-down
```

Remove o namespace e desliga o perfil `caas`. Para o loop rápido de desenvolvimento, sem Kubernetes, use `make up` / `make down`.
