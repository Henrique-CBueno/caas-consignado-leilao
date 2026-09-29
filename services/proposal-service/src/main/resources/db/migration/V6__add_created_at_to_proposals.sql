-- Data de criação para listar as propostas do tenant da mais recente para a mais antiga (Milestone 19).
-- Propostas anteriores a esta migração recebem a data da migração (limitação aceita, ADR-0031).
ALTER TABLE proposals ADD COLUMN created_at TIMESTAMPTZ NOT NULL DEFAULT now();
