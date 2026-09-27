-- Mesmo mecanismo dos demais serviços: o login role do Testcontainers/RDS emulado
-- é superusuário e ignora RLS por padrão, mesmo com FORCE ROW LEVEL SECURITY.
CREATE ROLE app_role NOSUPERUSER NOBYPASSRLS NOLOGIN;

GRANT SELECT, INSERT, UPDATE, DELETE ON disbursements TO app_role;
