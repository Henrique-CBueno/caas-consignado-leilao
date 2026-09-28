# C4 — Containers

Visão de containers no estilo C4, desenhada como fluxograma por legibilidade (o layout automático do `C4Container` do Mermaid sobrepõe rótulos com tantas relações). Setas tracejadas são eventos Kafka; setas contínuas são chamadas síncronas ou acesso a dados.

```mermaid
flowchart LR
    analista(["Analista do banco"])
    jev["Jev / OpenRouter<br/>(externo)"]
    cognito["cognito-local<br/>(JWKS)"]

    subgraph k8s["Kubernetes (minikube, perfil caas)"]
        direction LR
        dashboard["Dashboard<br/>Angular + STOMP"]
        gateway["api-gateway<br/>JWT, roteamento,<br/>Circuit Breaker, rate limit"]
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
        obs["Jaeger, Prometheus, Grafana"]
    end

    analista --> dashboard
    analista --> gateway
    dashboard -- WebSocket --> notif
    gateway -- JWKS --> cognito
    gateway -- "/tenants/**" --> tenant
    gateway -- "/proposals/**" --> proposal

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
```

Todos os serviços exportam traces (OTLP) para o Jaeger e expõem métricas ao Prometheus (`obs`, ADR-0011); as setas foram omitidas para não poluir. Decomposição e dados por serviço: ADR-0012 e ADR-0016. O `credit-analysis-service` revalida o crédito quando o leilão fecha (etapa `POST_AUCTION`, ADR-0020).
