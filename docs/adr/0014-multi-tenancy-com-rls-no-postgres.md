# 0014 — Multi-tenancy com seed de tenants e RLS no Postgres

## Status
Aceita

## Contexto
A plataforma atende vários bancos (tenants). O isolamento entre eles é o risco mais caro de errar, e um filtro `WHERE tenant_id = ?` esquecido numa query vaza dados.

## Decisão
Um esquema compartilhado com coluna `tenant_id` e **Row-Level Security**: `ENABLE` e `FORCE ROW LEVEL SECURITY` com a política `tenant_isolation` (`tenant_id = current_setting('app.current_tenant', true)::uuid`), e um papel `app_role` (`NOSUPERUSER NOBYPASSRLS NOLOGIN`). Cada transação faz `SET LOCAL ROLE app_role` e `set_config('app.current_tenant', <id>, true)` — escopo de transação, seguro com o pool Hikari; sem o `SET LOCAL ROLE` o login role (dono da tabela ou superusuário) ignoraria a política. O tenant vem do header `X-Tenant-Id`, lido por um `TenantContextFilter`. Os tenants são dados de seed no `tenant-service` (Banco Alfa, Banco Beta, Fintech Gama). Tabelas puramente técnicas (`outbox_events`) ficam sem RLS porque o relay varre todos os tenants.

## Consequências
- Existe teste de integração de isolamento cross-tenant (uma consulta de um tenant nunca enxerga linhas de outro).
- O `auction-service` (DynamoDB) não tem RLS: o `tenant_id` é atributo do item, sem isolamento no armazenamento — limitação.
- O `X-Tenant-Id` é confiado ao gateway, que o deriva do claim do JWT e descarta o do cliente (ADR-0017); a chamada direta a um serviço interno é barrada por NetworkPolicy (ADR-0024). O smoke test prova o isolamento: o tenant A cria uma proposta, o tenant B recebe 404 para ela e um `X-Tenant-Id` forjado é ignorado.
