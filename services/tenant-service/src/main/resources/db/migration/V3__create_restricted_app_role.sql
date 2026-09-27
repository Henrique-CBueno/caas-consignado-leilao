-- Superusuários (inclusive o dono da tabela, mesmo com FORCE ROW LEVEL SECURITY)
-- sempre ignoram RLS. A aplicação assume esta role, sem privilégio de superusuário,
-- via "SET LOCAL ROLE" no início de cada transação (ver JpaTenantRepository),
-- para que as políticas de RLS realmente sejam aplicadas em runtime.
CREATE ROLE app_role NOSUPERUSER NOBYPASSRLS NOLOGIN;

GRANT SELECT, INSERT, UPDATE, DELETE ON tenants TO app_role;
