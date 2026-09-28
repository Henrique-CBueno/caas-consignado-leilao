# CaaS Consignado Leilão

Plataforma **simulada** de *Credit-as-a-Service* para leilão reverso de crédito consignado — projeto de portfólio técnico (não é um produto real). Uma proposta atravessa nove microsserviços por eventos Kafka: análise de crédito, leilão reverso entre financiadores (bots), revalidação, contrato e desembolso, com um trace distribuído único por proposta.

![Trace único de uma proposta no Jaeger](docs/img/jaeger-trace.png)

| Trace no Jaeger | Dashboard no Grafana | Leilão ao vivo (Angular) |
|---|---|---|
| ![Jaeger](docs/img/jaeger-trace.png) | ![Grafana](docs/img/grafana-dashboard.png) | ![Leilão](docs/img/dashboard-leilao.png) |

## O que este projeto demonstra

- **Arquitetura orientada a eventos** com Kafka, transactional outbox (Postgres e DynamoDB) e correlação de eventos fora de ordem ([ADR-0003](docs/adr/0003-transactional-outbox.md), [ADR-0007](docs/adr/0007-correlacao-de-eventos-no-contract-service.md)).
- **Clean Architecture e DDD** em todos os serviços ([ADR-0013](docs/adr/0013-clean-architecture-e-ddd.md)) e **multi-tenancy com Row-Level Security** ([ADR-0014](docs/adr/0014-multi-tenancy-com-rls-no-postgres.md)).
- **Decisão de crédito por IA** (Jev/OpenRouter) atrás de uma porta, com trava de confiança e degradação graciosa ([ADR-0005](docs/adr/0005-integracao-jev-openrouter-e-vault.md), [ADR-0020](docs/adr/0020-trava-de-confianca-e-degradacao-do-credito.md)).
- **Resiliência**: Circuit Breaker e rate limit (Resilience4j, Spring Cloud Gateway) ([ADR-0021](docs/adr/0021-spring-cloud-gateway-roteamento-e-rate-limit.md), [ADR-0022](docs/adr/0022-resilience4j-nas-chamadas-de-saida.md)).
- **Kubernetes real**: minikube, Helm (Kafka, Zookeeper, Vault), StatefulSets de Postgres por serviço ([ADR-0024](docs/adr/0024-kubernetes-statefulsets-helm-e-vault-em-modo-dev.md)); IaC com Terraform contra LocalStack ([ADR-0023](docs/adr/0023-terraform-e-localstack.md)).
- **Observabilidade**: trace único (OpenTelemetry → Jaeger) atravessando outbox, Kafka e HTTP; métricas Prometheus e dashboard Grafana ([ADR-0011](docs/adr/0011-observabilidade-traces-e-metricas.md)).
- **Testes em três níveis** (unitário, integração com Testcontainers, contrato com Pact e OpenAPI sem deriva) ([ADR-0010](docs/adr/0010-estrategia-de-testes-em-tres-niveis.md)).
- **Metodologia**: spec-first + TDD, com issues como especificação ([ADR-0026](docs/adr/0026-metodologia-spec-first-e-tdd.md)).

## Arquitetura

Diagramas C4 em Mermaid: [contexto](docs/c4/context.md), [containers](docs/c4/container.md), [componentes do `proposal-service`](docs/c4/component-proposal-service.md) e [sequência do fluxo feliz](docs/c4/sequencia-fluxo-feliz.md). Linguagem do domínio em [CONTEXT.md](CONTEXT.md). Contratos REST em [`docs/openapi/`](docs/openapi/) (gerados do código). Todas as decisões em [docs/adr/](docs/adr/README.md).

| Serviço | Papel |
|---|---|
| `api-gateway` | Borda pública: JWT, tenant derivado do claim, roteamento, Circuit Breaker, rate limit por tenant |
| `tenant-service` | Tenants (seed) |
| `proposal-service` | Originação de propostas |
| `credit-analysis-service` | Decisão de crédito pré e pós-leilão |
| `auction-service` | Leilão reverso e desempate (DynamoDB) |
| `funder-bot-service` | Bots financiadores |
| `notification-gateway-service` | Eventos → WebSocket (STOMP) |
| `contract-service` | Correlaciona leilão e revalidação; assina contrato |
| `disbursement-service` | Desembolso simulado |
| `dashboard/` | Angular: leilão ao vivo |

## Como rodar

Pré-requisitos: Docker, `minikube`, `kubectl`, `helm`, JDK 21, Node 22 e cerca de 8 GiB livres para o cluster.

```
make deploy-local   # sobe o cluster completo (perfil minikube "caas") e todos os serviços
make smoke-test     # fluxo feliz pelo gateway com token real + isolamento de tenants + trace + métricas
make token          # ID token de um usuário de seed (TENANT_USER=alfa|beta|gama) para chamar o gateway
make k8s-down       # desliga e limpa o cluster
```

Roteiro completo para uma demonstração ao vivo: [docs/demo.md](docs/demo.md).

Desenvolvimento do dia a dia:

```
make up             # Kafka, Zookeeper, LocalStack e Vault leves via docker compose
make down           # derruba e limpa volumes
make test           # unitários, integração (Testcontainers) e contrato
make docs-check     # links, ADRs e alvos make da documentação
```

## Testes

`make test` roda três níveis, separados por sufixo de classe ([ADR-0010](docs/adr/0010-estrategia-de-testes-em-tres-niveis.md)): unitário, `*IntegrationTest` (Testcontainers: Postgres, Kafka, LocalStack, cognito-local, Vault) e `*ContractTest` (Pact entre `funder-bot-service` e `auction-service`; teste de que a OpenAPI commitada não divergiu do código).

## Limitações conhecidas

Ditas com franqueza; cada uma está registrada num ADR:

- **O tenant só é derivado do token para o ID token** (claim `custom:tenant_id`); em produção o access token exigiria uma Lambda de pré-geração de token. O **WebSocket** do `notification-gateway-service` segue público e sem autenticação ([ADR-0017](docs/adr/0017-autenticacao-com-cognito-emulado.md)).
- **O isolamento de rede depende do CNI Calico** do perfil `caas` (`--cni=calico`); um perfil criado antes dessa mudança precisa ser recriado com `minikube delete -p caas` ([ADR-0024](docs/adr/0024-kubernetes-statefulsets-helm-e-vault-em-modo-dev.md)).
- **O `cd-minikube` é manual** (`workflow_dispatch`): o runner gratuito do GitHub (2 vCPU / 7 GB) não sustenta o cluster, nem na topologia reduzida ([ADR-0009](docs/adr/0009-topologia-slim-no-ci.md)). O `ci.yml` (testes, `terraform plan` e `docs-check`) roda a cada push.
- **O `demo-smoke`, que exercita a API real do Jev, depende de um segredo (`OPENROUTER_API_KEY`) e não foi validado sem ele.** Nos testes e no cluster local a decisão de crédito usa um adaptador simulado determinístico ([ADR-0005](docs/adr/0005-integracao-jev-openrouter-e-vault.md)).
- **Vault em modo dev** (sem persistência, token fixo) e **Jaeger/Prometheus sem persistência** ([ADR-0024](docs/adr/0024-kubernetes-statefulsets-helm-e-vault-em-modo-dev.md), [ADR-0011](docs/adr/0011-observabilidade-traces-e-metricas.md)).
- **Lances concorrentes** usam *last-write-wins* (sem update condicional no DynamoDB) ([ADR-0018](docs/adr/0018-mecanismo-de-leilao-e-desempate.md)).
- `MANUAL_REVIEW` é um estado terminal: não há fila de análise humana.
