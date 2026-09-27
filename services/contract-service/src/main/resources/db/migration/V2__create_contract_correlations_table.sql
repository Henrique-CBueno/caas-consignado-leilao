-- Correlaciona, por proposal_id, a revalidação de crédito pós-leilão aprovada
-- com o fechamento do leilão com vencedor — os dois eventos podem chegar em
-- qualquer ordem; o contrato só é gerado quando ambos estiverem presentes
-- (ver ADR do mecanismo de correlação da Milestone 8).
CREATE TABLE contract_correlations (
    proposal_id UUID PRIMARY KEY,
    tenant_id UUID NOT NULL,
    requested_amount NUMERIC(15, 2),
    winning_funder_id TEXT,
    winning_rate NUMERIC(6, 2),
    winning_term_months INTEGER,
    contract_generated BOOLEAN NOT NULL DEFAULT FALSE
);
