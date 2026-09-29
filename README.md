# CaaS Consignado Leilão

Plataforma **simulada** de *Credit-as-a-Service* para leilão reverso de crédito consignado — projeto de portfólio técnico (não é um produto real). Uma proposta atravessa nove microsserviços por eventos Kafka: análise de crédito, leilão reverso entre financiadores (bots), revalidação, contrato e desembolso, com um trace distribuído único por proposta.

![Trace único de uma proposta no Jaeger](docs/img/jaeger-trace.png)

| Trace no Jaeger | Dashboard no Grafana | Leilão ao vivo (Angular) |
|---|---|---|
| ![Jaeger](docs/img/jaeger-trace.png) | ![Grafana](docs/img/grafana-dashboard.png) | ![Leilão](docs/img/dashboard-leilao.png) |

| Entrar (seletor de tenant) | Nova proposta | Administração de tenants |
|---|---|---|
| ![Entrar](docs/img/dashboard-entrar.png) | ![Nova proposta](docs/img/dashboard-nova-proposta.png) | ![Administração](docs/img/dashboard-admin.png) |

Fluxo completo no navegador, gravado no cluster real: o administrador cria um banco, entra como ele, cria uma proposta e acompanha o leilão até o vencedor.

![Fluxo completo: administrador cria tenant, entra como ele, cria proposta e acompanha o leilão](docs/img/demo-fluxo-completo.gif)

## O que este projeto demonstra

- **Arquitetura orientada a eventos** com Kafka, transactional outbox (Postgres e DynamoDB) e correlação de eventos fora de ordem ([ADR-0003](docs/adr/0003-transactional-outbox.md), [ADR-0007](docs/adr/0007-correlacao-de-eventos-no-contract-service.md)).
- **Clean Architecture e DDD** em todos os serviços ([ADR-0013](docs/adr/0013-clean-architecture-e-ddd.md)) e **multi-tenancy com Row-Level Security** ([ADR-0014](docs/adr/0014-multi-tenancy-com-rls-no-postgres.md)).
- **Decisão de crédito por IA** (Jev/OpenRouter) atrás de uma porta, com trava de confiança e degradação graciosa ([ADR-0005](docs/adr/0005-integracao-jev-openrouter-e-vault.md), [ADR-0020](docs/adr/0020-trava-de-confianca-e-degradacao-do-credito.md)).
- **Resiliência**: Circuit Breaker e rate limit (Resilience4j, Spring Cloud Gateway) ([ADR-0021](docs/adr/0021-spring-cloud-gateway-roteamento-e-rate-limit.md), [ADR-0022](docs/adr/0022-resilience4j-nas-chamadas-de-saida.md)).
- **Kubernetes real**: minikube, Helm (Kafka, Zookeeper, Vault), StatefulSets de Postgres por serviço ([ADR-0024](docs/adr/0024-kubernetes-statefulsets-helm-e-vault-em-modo-dev.md)); IaC com Terraform contra LocalStack ([ADR-0023](docs/adr/0023-terraform-e-localstack.md)).
- **Observabilidade**: trace único (OpenTelemetry → Jaeger) atravessando outbox, Kafka e HTTP; métricas Prometheus e dashboard Grafana ([ADR-0011](docs/adr/0011-observabilidade-traces-e-metricas.md)).
- **Testes em três níveis** (unitário, integração com Testcontainers, contrato com Pact e OpenAPI sem deriva) ([ADR-0010](docs/adr/0010-estrategia-de-testes-em-tres-niveis.md)).
- **Papéis e isolamento de ponta a ponta**: login de demonstração pelo gateway, papel administrativo que só atravessa a RLS por uma role de privilégio mínimo (`admin_role`: só ler e inserir em `tenants`) e tenants criados pela interface com usuário próprio ([ADR-0028](docs/adr/0028-login-de-demonstracao-e-criacao-de-proposta-no-front.md), [ADR-0029](docs/adr/0029-papel-administrativo-e-painel-de-tenants.md)).
- **Isolamento no tempo real**: o WebSocket exige o ID token no `CONNECT`, os tópicos são por tenant e o CORS aceita só a origem do dashboard ([ADR-0030](docs/adr/0030-websocket-autenticado-por-tenant-e-cors-restrito.md)).
- **Metodologia**: spec-first + TDD, com issues como especificação ([ADR-0026](docs/adr/0026-metodologia-spec-first-e-tdd.md)).

## Arquitetura

Diagramas C4 em Mermaid: [contexto](docs/c4/context.md), [containers](docs/c4/container.md), [componentes do `proposal-service`](docs/c4/component-proposal-service.md) e [sequência do fluxo feliz](docs/c4/sequencia-fluxo-feliz.md). Linguagem do domínio em [CONTEXT.md](CONTEXT.md). Contratos REST em [`docs/openapi/`](docs/openapi/) (gerados do código). Todas as decisões em [docs/adr/](docs/adr/README.md).

| Serviço | Papel |
|---|---|
| `api-gateway` | Borda pública: login de demonstração (`/auth/login`), JWT, tenant derivado do claim, papel administrativo em `/admin/**`, roteamento, Circuit Breaker, rate limit por tenant, CORS das rotas do front |
| `tenant-service` | Tenants (seed) e API administrativa (listar/criar tenant e seu usuário demo) |
| `proposal-service` | Originação de propostas |
| `credit-analysis-service` | Decisão de crédito pré e pós-leilão |
| `auction-service` | Leilão reverso e desempate (DynamoDB) |
| `funder-bot-service` | Bots financiadores |
| `notification-gateway-service` | Eventos → WebSocket (STOMP) autenticado, um tópico por tenant |
| `contract-service` | Correlaciona leilão e revalidação; assina contrato |
| `disbursement-service` | Desembolso simulado |
| `dashboard/` | Angular: leilão ao vivo, histórico local, entrar, nova proposta e administração de tenants |

## Como rodar

Pré-requisitos: Docker, `minikube`, `kubectl`, `helm`, JDK 21, Node 22 e cerca de 8 GiB livres para o cluster.

```
make deploy-local   # sobe o cluster completo (perfil minikube "caas") e todos os serviços
make smoke-test     # fluxo feliz pelo gateway com token real + isolamento de tenants + trace + métricas
make token          # ID token de um usuário de seed (TENANT_USER=alfa|beta|gama) para chamar o gateway
make k8s-down       # desliga e limpa o cluster
```

Depois do deploy, o dashboard fica em `http://$(minikube -p caas ip):30090`: **Entrar** (Banco Alfa, Banco Beta, Fintech Gama ou administrador; a senha demo é fixa, ADR-0017), **Nova proposta** e, como administrador, **Administração** para criar um tenant novo e entrar como ele. Roteiro completo para uma demonstração ao vivo: [docs/demo.md](docs/demo.md).

Desenvolvimento do dia a dia:

```
make up             # Kafka, Zookeeper, LocalStack e Vault leves via docker compose
make down           # derruba e limpa volumes
make test           # unitários, integração (Testcontainers) e contrato
make test-front    # testes do dashboard Angular (Vitest + jsdom)
make docs-check     # links, ADRs e alvos make da documentação
```

## Testes

O front tem testes próprios (`make test-front`, Vitest + jsdom, 55 testes de comportamento pelo DOM; ver [ADR-0027](docs/adr/0027-front-rack-de-tiras-e-feed-injetavel.md)). `make test` roda três níveis do backend, separados por sufixo de classe ([ADR-0010](docs/adr/0010-estrategia-de-testes-em-tres-niveis.md)): unitário, `*IntegrationTest` (Testcontainers: Postgres, Kafka, LocalStack, cognito-local, Vault) e `*ContractTest` (Pact entre `funder-bot-service` e `auction-service`; teste de que a OpenAPI commitada não divergiu do código).

## Limitações conhecidas

Ditas com franqueza; cada uma está registrada num ADR:

- **O tenant só é derivado do token para o ID token** (claim `custom:tenant_id`); em produção o access token exigiria uma Lambda de pré-geração de token ([ADR-0017](docs/adr/0017-autenticacao-com-cognito-emulado.md)). O WebSocket exige o ID token, mas sem TLS neste ambiente o token trafega em claro, e a conexão aberta não acompanha a expiração do token ([ADR-0030](docs/adr/0030-websocket-autenticado-por-tenant-e-cors-restrito.md)).
- **Login de demonstração**: um usuário por tenant com senha fixa e documentada; o seletor de entrada só conhece os tenants criados no mesmo navegador (não existe listagem pública) ([ADR-0028](docs/adr/0028-login-de-demonstracao-e-criacao-de-proposta-no-front.md), [ADR-0029](docs/adr/0029-papel-administrativo-e-painel-de-tenants.md)).
- **Criação de tenant sem transação distribuída**: se o provedor de identidade falhar, a API responde 502 e o tenant fica gravado sem usuário demo; repetir o pedido o completa ([ADR-0029](docs/adr/0029-papel-administrativo-e-painel-de-tenants.md), [ADR-0030](docs/adr/0030-websocket-autenticado-por-tenant-e-cors-restrito.md)). Um pool do `cognito-local` anterior à Milestone 17 precisa ser recriado.
- **O isolamento de rede depende do CNI Calico** do perfil `caas` (`--cni=calico`); um perfil criado antes dessa mudança precisa ser recriado com `minikube delete -p caas` ([ADR-0024](docs/adr/0024-kubernetes-statefulsets-helm-e-vault-em-modo-dev.md)).
- **O `cd-minikube` é manual** (`workflow_dispatch`): o runner gratuito do GitHub (2 vCPU / 7 GB) não sustenta o cluster, nem na topologia reduzida ([ADR-0009](docs/adr/0009-topologia-slim-no-ci.md)). O `ci.yml` (testes, `terraform plan` e `docs-check`) roda a cada push.
- **O `demo-smoke`, que exercita a API real do Jev, depende de um segredo (`OPENROUTER_API_KEY`) e não foi validado sem ele.** Nos testes e no cluster local a decisão de crédito usa um adaptador simulado determinístico ([ADR-0005](docs/adr/0005-integracao-jev-openrouter-e-vault.md)).
- **Vault em modo dev** (sem persistência, token fixo) e **Jaeger/Prometheus sem persistência** ([ADR-0024](docs/adr/0024-kubernetes-statefulsets-helm-e-vault-em-modo-dev.md), [ADR-0011](docs/adr/0011-observabilidade-traces-e-metricas.md)).
- **Lances concorrentes** usam *last-write-wins* (sem update condicional no DynamoDB) ([ADR-0018](docs/adr/0018-mecanismo-de-leilao-e-desempate.md)).
- `MANUAL_REVIEW` é um estado terminal: não há fila de análise humana.
