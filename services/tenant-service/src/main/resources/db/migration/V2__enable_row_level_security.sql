ALTER TABLE tenants ENABLE ROW LEVEL SECURITY;
ALTER TABLE tenants FORCE ROW LEVEL SECURITY;

-- Uma sessão só enxerga a linha cujo id bate com o tenant do contexto atual
-- (app.current_tenant, setado por transação via set_config em JpaTenantRepository).
-- current_setting(..., true) retorna NULL se o contexto não foi setado, e
-- "id = NULL" nunca é verdadeiro: sem contexto, sem acesso (default-deny).
CREATE POLICY tenant_isolation ON tenants
    USING (id = current_setting('app.current_tenant', true)::uuid);
