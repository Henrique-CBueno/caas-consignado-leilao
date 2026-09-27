ALTER TABLE credit_decisions ENABLE ROW LEVEL SECURITY;
ALTER TABLE credit_decisions FORCE ROW LEVEL SECURITY;

-- Ver ADR de RLS do tenant-service/proposal-service para o racional completo.
CREATE POLICY tenant_isolation ON credit_decisions
    USING (tenant_id = current_setting('app.current_tenant', true)::uuid);
