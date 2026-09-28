# 0023 — Infraestrutura como código com Terraform contra LocalStack, e o que mudaria em produção

## Status
Aceita

## Contexto
Queremos mostrar IaC sem conta de nuvem. O LocalStack Community só emula de verdade DynamoDB e S3 (ADR-0002); RDS, Cognito e outros recursos são Pro ou incompletos.

## Decisão
O Terraform (provider `aws ~> 5.0` apontado para o endpoint do LocalStack) provisiona apenas o que o LocalStack emula de verdade: as tabelas DynamoDB `auctions` (chave `proposal_id`) e `outbox_events` (chave `id`), com cobrança sob demanda. O LocalStack tem **dois papéis**: alvo do Terraform (contêiner avulso, no CI e localmente) e dependência de runtime do `auction-service` (uma instância dentro do cluster, ver ADR-0024). Em runtime local o `DynamoDbSchemaInitializer` garante as tabelas de forma idempotente; o Terraform documenta o IaC equivalente, e essa duplicação é assumida. O `terraform plan` roda no `ci.yml`; o `apply` roda no workflow manual (ADR-0009).

## Consequências
- Um único root module, sem a árvore `modules/` e `envs/` do plano (YAGNI enquanto há um só ambiente).
- Em produção: DynamoDB e RDS reais, Cognito e Secrets Manager/Vault, estado remoto (S3 + lock), módulos por ambiente e `plan` comentado em PR.
