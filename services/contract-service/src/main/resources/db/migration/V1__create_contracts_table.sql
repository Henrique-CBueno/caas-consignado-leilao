CREATE TABLE contracts (
    id UUID PRIMARY KEY,
    proposal_id UUID NOT NULL,
    tenant_id UUID NOT NULL,
    funder_id TEXT NOT NULL,
    rate NUMERIC(6, 2) NOT NULL,
    term_months INTEGER NOT NULL,
    amount NUMERIC(15, 2) NOT NULL,
    signed_at TIMESTAMPTZ NOT NULL
);
