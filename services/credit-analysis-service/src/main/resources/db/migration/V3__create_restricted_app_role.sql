-- Login role do Testcontainers/RDS emulado é superusuário e ignora RLS por
-- padrão, mesmo com FORCE ROW LEVEL SECURITY (ver ADR de RLS do tenant-service).
CREATE ROLE app_role NOSUPERUSER NOBYPASSRLS NOLOGIN;

GRANT SELECT, INSERT, UPDATE, DELETE ON credit_decisions TO app_role;
