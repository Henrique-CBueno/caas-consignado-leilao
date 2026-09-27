ALTER TABLE disbursements ENABLE ROW LEVEL SECURITY;
ALTER TABLE disbursements FORCE ROW LEVEL SECURITY;
CREATE POLICY tenant_isolation ON disbursements
    USING (tenant_id = current_setting('app.current_tenant', true)::uuid);
