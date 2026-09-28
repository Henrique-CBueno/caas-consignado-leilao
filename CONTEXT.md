# Contexto do domínio — CaaS Consignado Leilão

Linguagem usada no código, nos eventos e na documentação. Termos em inglês são os nomes de tipos e campos; ver [ADRs](docs/adr/README.md) para as decisões por trás.

| Termo | Significado |
|---|---|
| **Tenant** | Banco que usa a plataforma (ex.: Banco Alfa). Todo dado de negócio pertence a um tenant e é isolado por RLS ([ADR-0014](docs/adr/0014-multi-tenancy-com-rls-no-postgres.md)). |
| **Proposta** (`Proposal`) | Pedido de crédito consignado: `borrowerId`, `requestedAmount`, `termMonths`. Nasce em `PENDING_CREDIT_ANALYSIS`. |
| **Decisão de crédito** (`CreditDecision`) | Resultado da análise: `APPROVE`, `REJECT` ou `MANUAL_REVIEW`, com `confidence` (0–1). Confiança abaixo de 0,6 vira `MANUAL_REVIEW` ([ADR-0020](docs/adr/0020-trava-de-confianca-e-degradacao-do-credito.md)). |
| **Etapa** (`stage`) | `PRE_AUCTION` (decide se a proposta vai a leilão) ou `POST_AUCTION` (revalida o vencedor antes do contrato). |
| **Leilão reverso** (`Auction`) | Financiadores competem pela **menor taxa**. Abre após aprovação pré-leilão e fecha ao fim da janela ([ADR-0018](docs/adr/0018-mecanismo-de-leilao-e-desempate.md)). |
| **Lance** (`Bid`) | Oferta de um financiador: `funderId`, `rate` (taxa), `termMonths` (prazo). |
| **Financiador** (`funder`) | Quem dá lances. Aqui, bots simulados ([ADR-0019](docs/adr/0019-financiadores-simulados-como-servico.md)). |
| **Vencedor** | Lance de menor taxa; empate → menor prazo; novo empate → lance mais antigo. |
| **Contrato** (`Contract`) | Formalizado quando o leilão fechou com vencedor **e** a revalidação pós-leilão aprovou, em qualquer ordem de chegada ([ADR-0007](docs/adr/0007-correlacao-de-eventos-no-contract-service.md)). |
| **Desembolso** (`Disbursement`) | Pagamento simulado do valor ao tomador, gerado a partir do contrato assinado; status `DISBURSED`. |
| **Evento de domínio** | Fato publicado no Kafka: `ProposalCreated`, `CreditDecisionMade`, `AuctionOpened`, `AuctionBidPlaced`, `AuctionClosed`, `ContractSigned`. |
| **Outbox** | Tabela/coleção onde o evento é gravado junto com o dado e publicado depois por um relay ([ADR-0003](docs/adr/0003-transactional-outbox.md)). |
| **Degradação graciosa** | Falha do provedor de crédito vira `MANUAL_REVIEW` em vez de erro ([ADR-0022](docs/adr/0022-resilience4j-nas-chamadas-de-saida.md)). |
| **Trace** | Rastro distribuído de uma proposta ponta a ponta, visível no Jaeger ([ADR-0011](docs/adr/0011-observabilidade-traces-e-metricas.md)). |
