-- Papel administrativo da plataforma (Milestone 17, ADR-0029): único que atravessa a RLS de
-- tenants, para listar e cadastrar tenants. Privilégio mínimo: só SELECT e INSERT (nunca
-- UPDATE/DELETE), usado apenas por JpaAdminTenantRepository.
CREATE ROLE admin_role NOSUPERUSER BYPASSRLS NOLOGIN;

GRANT SELECT, INSERT ON tenants TO admin_role;

CREATE UNIQUE INDEX tenants_name_key ON tenants (name);
