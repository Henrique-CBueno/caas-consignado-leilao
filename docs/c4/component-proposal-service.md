# C4 — Componentes do `proposal-service`

O `proposal-service` é o exemplo canônico da Clean Architecture aplicada em todos os serviços (ADR-0013): a dependência aponta sempre para dentro — a infraestrutura depende da aplicação (implementa as portas), e a aplicação depende do domínio.

```mermaid
flowchart TB
    cliente(["api-gateway<br/>(X-Tenant-Id)"])
    kafka{{"Kafka<br/>proposal.created"}}
    pg[("Postgres<br/>proposals + outbox_events<br/>RLS")]

    subgraph infra["infrastructure"]
        direction LR
        web["ProposalController<br/>TenantContextFilter"]
        jpa["JpaProposalRepository<br/>SET LOCAL ROLE app_role +<br/>set_config('app.current_tenant')"]
        jpaOutbox["JpaOutboxEventPublisher<br/>grava outbox com traceparent"]
        relay["OutboxRelay<br/>@Scheduled, restaura o trace"]
    end

    subgraph app["application"]
        direction LR
        usecase["CreateProposalUseCase"]
        repoPort(["ProposalRepository<br/>porta"])
        outboxPort(["OutboxEventPublisher<br/>porta"])
    end

    subgraph dom["domain"]
        domain["Proposal, ProposalId, TenantId<br/>records, sem Spring"]
    end

    cliente --> web
    web --> usecase
    usecase --> repoPort
    usecase --> outboxPort
    usecase --> domain
    jpa -. implementa .-> repoPort
    jpaOutbox -. implementa .-> outboxPort
    jpa --> pg
    jpaOutbox --> pg
    relay -- "varre pendentes" --> pg
    relay --> kafka
```
