# C4 — Containers

Visão de containers no estilo C4, desenhada como fluxograma por legibilidade (o layout automático do `C4Container` do Mermaid sobrepõe rótulos com tantas relações). Setas tracejadas são eventos Kafka; setas contínuas são chamadas síncronas ou acesso a dados.

```mermaid
flowchart LR
    analista(["Analista do banco"])
    jev["Jev / OpenRouter<br/>(externo)"]

    subgraph k8s["Kubernetes (minikube, perfil caas)"]
        direction LR
        dashboard["Dashboard<br/>Angular + STOMP"]
        gateway["api-gateway<br/>JWT, tenant do claim,<br/>Circuit Breaker, rate limit por tenant"]
        tenant["tenant-service"]
        proposal["proposal-service"]
        credit["credit-analysis-service"]
        auction["auction-service"]
        bot["funder-bot-service"]
        notif["notification-gateway-service<br/>STOMP"]
        contract["contract-service"]
        disb["disbursement-service"]

        kafka{{"Kafka + Zookeeper"}}
        pg[("Postgres por serviço<br/>RLS")]
        dynamo[("DynamoDB<br/>LocalStack")]
        vault["Vault (modo dev)"]
        cognito["cognito-local<br/>usuários dos 3 tenants de seed e do admin"]
        obs["Jaeger, Prometheus, Grafana"]
    end

    analista --> dashboard
    analista --> gateway
    dashboard -- WebSocket --> notif
    gateway -- JWKS --> cognito
    gateway -- "/tenants/**, /admin/**" --> tenant
    gateway -- "/proposals/**" --> proposal
    gateway -- "/disbursements/**" --> disb

    proposal -. "proposal.created" .-> kafka
    kafka -. "proposal.created<br/>auction.closed" .-> credit
    credit -. "credit.decision.made" .-> kafka
    kafka -. "credit.decision.made" .-> auction
    auction -. "auction.opened<br/>auction.bid.placed<br/>auction.closed" .-> kafka
    kafka -. "auction.opened" .-> bot
    bot -- "POST /auctions/{id}/bids" --> auction
    kafka -. "auction.bid.placed<br/>auction.closed" .-> notif
    kafka -. "auction.closed<br/>credit.decision.made" .-> contract
    contract -. "contract.signed" .-> kafka
    kafka -. "contract.signed" .-> disb

    credit -- "decisão de crédito" --> jev
    credit -- "segredo" --> vault
    auction --> dynamo
    proposal --> pg
    credit --> pg
    contract --> pg
    disb --> pg
    tenant --> pg
    tenant -. "usuário demo do tenant novo" .-> cognito
```

As NetworkPolicies (Calico) só admitem tráfego para `tenant-service`, `proposal-service` e `disbursement-service` vindo do `api-gateway`, e para o `auction-service` vindo do `funder-bot-service` ([ADR-0024](../adr/0024-kubernetes-statefulsets-helm-e-vault-em-modo-dev.md)); o gateway injeta o `X-Tenant-Id` a partir do claim do token ([ADR-0017](../adr/0017-autenticacao-com-cognito-emulado.md)). Todos os serviços exportam traces (OTLP) para o Jaeger e expõem métricas ao Prometheus (`obs`, ADR-0011); as setas foram omitidas para não poluir. Decomposição e dados por serviço: ADR-0012 e ADR-0016. O `credit-analysis-service` revalida o crédito quando o leilão fecha (etapa `POST_AUCTION`, ADR-0020).
