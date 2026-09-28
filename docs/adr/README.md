# Architecture Decision Records

Decisões de arquitetura do CaaS Consignado Leilão. Cada ADR tem **Status**, **Contexto**, **Decisão** e **Consequências** (verificado por `make docs-check`). Onde o plano original divergiu da realidade da implementação, vale a realidade — e o desvio está registrado no ADR correspondente.

## Índice

| ADR | Tema |
|---|---|
| [0001](0001-monorepo-gradle-multimodulo.md) | Monorepo Gradle multimódulo |
| [0002](0002-limitacoes-localstack-community.md) | Limitações do LocalStack Community |
| [0003](0003-transactional-outbox.md) | Transactional outbox |
| [0004](0004-java-em-vez-de-kotlin.md) | Java em vez de Kotlin |
| [0005](0005-integracao-jev-openrouter-e-vault.md) | Integração com Jev/OpenRouter e Vault |
| [0006](0006-notification-gateway-servlet-stomp-em-vez-de-webflux.md) | Notification gateway: Servlet + STOMP em vez de WebFlux |
| [0007](0007-correlacao-de-eventos-no-contract-service.md) | Correlação de eventos no `contract-service` |
| [0008](0008-imagens-bitnami-legacy-para-kafka-zookeeper.md) | Imagens `bitnamilegacy` para Kafka + Zookeeper |
| [0009](0009-topologia-slim-no-ci.md) | Topologia slim e limites do CI gratuito |
| [0010](0010-estrategia-de-testes-em-tres-niveis.md) | Estratégia de testes em três níveis |
| [0011](0011-observabilidade-traces-e-metricas.md) | Observabilidade: traces e métricas |
| [0012](0012-decomposicao-em-bounded-contexts.md) | Decomposição em bounded contexts |
| [0013](0013-clean-architecture-e-ddd.md) | Clean Architecture e DDD |
| [0014](0014-multi-tenancy-com-rls-no-postgres.md) | Multi-tenancy com RLS |
| [0015](0015-criterio-de-migracao-para-schema-dedicado.md) | Critério de migração para schema dedicado |
| [0016](0016-persistencia-poliglota-postgres-e-dynamodb.md) | Persistência poliglota |
| [0017](0017-autenticacao-com-cognito-emulado.md) | Autenticação com Cognito emulado |
| [0018](0018-mecanismo-de-leilao-e-desempate.md) | Mecanismo do leilão e desempate |
| [0019](0019-financiadores-simulados-como-servico.md) | Financiadores simulados |
| [0020](0020-trava-de-confianca-e-degradacao-do-credito.md) | Trava de confiança e degradação do crédito |
| [0021](0021-spring-cloud-gateway-roteamento-e-rate-limit.md) | Gateway: roteamento e rate limit |
| [0022](0022-resilience4j-nas-chamadas-de-saida.md) | Resilience4j nas chamadas de saída |
| [0023](0023-terraform-e-localstack.md) | Terraform e LocalStack |
| [0024](0024-kubernetes-statefulsets-helm-e-vault-em-modo-dev.md) | Kubernetes, StatefulSets, Helm e Vault dev |
| [0025](0025-escopo-do-shared-kernel.md) | Escopo do shared kernel |
| [0026](0026-metodologia-spec-first-e-tdd.md) | Metodologia spec-first e TDD |
| [0027](0027-front-rack-de-tiras-e-feed-injetavel.md) | Front: feed injetável, modo demonstração e Rack de tiras |

## Mapa dos 26 temas do plano

O plano original numerava 26 ADRs; a numeração real segue a ordem em que as decisões foram tomadas.

| Tema do plano | ADR real |
|---|---|
| 1. Monorepo Gradle multimódulo | 0001 |
| 2. Decomposição em bounded contexts | 0012 |
| 3. Clean Architecture + DDD | 0013 |
| 4. Multi-tenancy com seed + RLS | 0014 |
| 5. Critério de migração por tenant | 0015 |
| 6. Persistência poliglota | 0016 |
| 7. Autenticação com Cognito emulado | 0017 |
| 8. Mecanismo de leilão e desempate | 0018 |
| 9. Funders simulados + injeção manual | 0019 |
| 10. Integração de crédito com Jev | 0005 |
| 11. `CreditDecisionPort` com adapters Jev e Mock | 0005 |
| 12. Trava de confidence < 60% | 0020 |
| 13. Kafka clássico com Zookeeper | 0008 |
| 14. WebSocket para UI vs Kafka para eventos | 0006 |
| 15. Gateway com roteamento e rate limit | 0021 |
| 16. Resilience4j nas chamadas de saída | 0022 |
| 17. Estratégia de testes em três níveis | 0010 |
| 18. Infraestrutura com LocalStack + Terraform | 0002, 0023 |
| 19. Kafka, Zookeeper e Vault em Kubernetes | 0024 |
| 20. Segredos via Vault em modo dev | 0005, 0024 |
| 21. Pipeline CI/CD | 0009 (e ADR 0010 para os níveis de teste) |
| 22. Observabilidade | 0011 |
| 23. Transactional outbox | 0003 |
| 24. Escopo do shared kernel | 0025 |
| 25. LocalStack fora do cluster | 0023 |
| 26. Metodologia spec-first + TDD | 0026 |

## ADRs que nasceram na implementação

0004 (Java em vez de Kotlin), 0007 (correlação de eventos no contract-service), 0008 (imagens legadas do Bitnami) e 0009 (topologia slim) registram achados reais que o plano não previa.
