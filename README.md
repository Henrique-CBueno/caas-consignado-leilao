# CaaS Consignado Leilão

Plataforma simulada de Credit-as-a-Service para leilão reverso de crédito consignado — projeto de portfólio técnico (não é um produto real).

Arquitetura completa, decisões e roadmap: [plano de implementação](docs/architecture-README.md) (a ser preenchido a partir da Milestone 14; até lá, ver ADRs em `docs/adr/`).

## Desenvolvimento

Metodologia: spec-first (`to-spec`) + TDD (`tdd`), skills do plugin `mattpocock-skills`. Setup: `/setup-matt-pocock-skills`.

```
make up      # sobe Kafka+Zookeeper, LocalStack e Vault (loop rápido de dev, ver infra/docker/docker-compose.dev.yml)
make down    # derruba e limpa volumes
make logs    # segue os logs
```

O ambiente demonstrado e testado em CI é o Kubernetes local (minikube + Helm) — ver Milestone 9 do plano.
