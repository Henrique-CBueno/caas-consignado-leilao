ALTER TABLE contracts ENABLE ROW LEVEL SECURITY;
ALTER TABLE contracts FORCE ROW LEVEL SECURITY;
CREATE POLICY tenant_isolation ON contracts
    USING (tenant_id = current_setting('app.current_tenant', true)::uuid);

ALTER TABLE contract_correlations ENABLE ROW LEVEL SECURITY;
ALTER TABLE contract_correlations FORCE ROW LEVEL SECURITY;
CREATE POLICY tenant_isolation ON contract_correlations
    USING (tenant_id = current_setting('app.current_tenant', true)::uuid);
