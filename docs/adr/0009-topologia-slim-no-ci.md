# 0009 — Topologia "slim" do cluster no CI (cd-minikube)

## Status
Aceita

## Contexto
O `cd-minikube.yml` sobe o cluster minikube inteiro dentro de um runner `ubuntu-latest` de repositório privado (2 vCPU / 7 GB). O deploy completo (9 serviços + dashboard + Vault + Kafka/ZK + 5 Postgres + LocalStack) consome ≈ 5,2 GiB só no container do minikube; somado ao runner, ao daemon do Gradle e ao LocalStack usado pelo `terraform apply`, o API server do minikube morria por falta de memória (achado empírico na Milestone 10: "connection refused" no meio do `kubectl wait`).

## Decisão
- `make deploy-local SLIM=1` sobe apenas o necessário ao smoke test (que fala com os serviços via port-forward/pod interno): proposal, credit-analysis, auction, funder-bot, contract e disbursement + seus Postgres, Kafka/ZK e LocalStack. Ficam de fora tenant-service, api-gateway, notification-gateway, dashboard e Vault. O deploy completo continua sendo o padrão local (`make deploy-local`).
- No workflow: o LocalStack do Terraform é removido após o `apply` e o Gradle roda sem daemon.
- Todos os pods JVM recebem `-Xmx192m -XX:+UseSerialGC` (`JAVA_TOOL_OPTIONS`) — efeito pequeno (~70 MiB no total), mas evita heap ilimitado guiado pela memória do host.

## Consequências
- O CI não exercita tenant-service/gateway/dashboard no cluster (cobertos por testes de integração e pelo `make smoke-test` completo local).
- Se mesmo assim faltar memória, as saídas são runner maior (pago) ou `workflow_dispatch` apenas.
