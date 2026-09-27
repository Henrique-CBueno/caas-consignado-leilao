ALTER TABLE proposals ENABLE ROW LEVEL SECURITY;
ALTER TABLE proposals FORCE ROW LEVEL SECURITY;

-- Diferente do tenant-service (onde a policy compara o próprio id), aqui é uma
-- coluna tenant_id de fato — ver ADR do tenant-service para o racional do mecanismo
-- (current_setting(..., true) retorna NULL sem contexto: default-deny).
CREATE POLICY tenant_isolation ON proposals
    USING (tenant_id = current_setting('app.current_tenant', true)::uuid);
