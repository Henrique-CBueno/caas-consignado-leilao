-- Tenants de demonstração com UUIDs fixos para reprodutibilidade em dev/CI/demo.
-- Rodando como o role dono da migration (superusuário), que ignora RLS por padrão,
-- então a inserção não precisa (nem pode, sem contexto) passar pela policy.
-- Campos de risco/regras de crédito e elegibilidade de funder ainda não existem no
-- agregado Tenant (YAGNI: entram quando auction-service/credit-analysis precisarem,
-- Milestones 2+) — este seed cobre só o que a Milestone 1 usa.
INSERT INTO tenants (id, name) VALUES
    ('11111111-1111-1111-1111-111111111111', 'Banco Alfa'),
    ('22222222-2222-2222-2222-222222222222', 'Banco Beta'),
    ('33333333-3333-3333-3333-333333333333', 'Fintech Gama');
