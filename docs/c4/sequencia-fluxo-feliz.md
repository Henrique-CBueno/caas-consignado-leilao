# Sequência — fluxo feliz (proposta → desembolso)

Toda publicação de evento passa pelo outbox transacional (ADR-0003); as setas para o Kafka já representam o relay. Um único trace atravessa toda a cadeia (ADR-0011).

```mermaid
sequenceDiagram
    autonumber
    actor A as Analista
    participant GW as api-gateway
    participant P as proposal-service
    participant K as Kafka
    participant C as credit-analysis-service
    participant AU as auction-service
    participant B as funder-bot-service
    participant N as notification-gateway
    participant CT as contract-service
    participant D as disbursement-service

    A->>GW: POST /proposals (JWT)
    GW->>P: POST /proposals (X-Tenant-Id)
    P->>K: proposal.created
    K->>C: consome (PRE_AUCTION)
    C->>K: credit.decision.made (APPROVE)
    K->>AU: consome
    AU->>K: auction.opened
    K->>B: consome
    par cada bot elegível
        B->>AU: POST /auctions/{id}/bids
        AU->>K: auction.bid.placed
        K->>N: reencaminha por STOMP
    end
    Note over AU: janela expira, AuctionCloser fecha e escolhe o vencedor
    AU->>K: auction.closed
    K->>C: consome (POST_AUCTION, revalida o vencedor)
    C->>K: credit.decision.made (POST_AUCTION)
    K->>CT: auction.closed + decisão pós-leilão
    CT->>K: contract.signed
    K->>D: consome
    D-->>A: GET /disbursements/{proposalId} = DISBURSED
```
