CREATE TABLE disbursements (
    proposal_id UUID PRIMARY KEY,
    tenant_id UUID NOT NULL,
    funder_id TEXT NOT NULL,
    amount NUMERIC(15, 2) NOT NULL,
    status TEXT NOT NULL,
    disbursed_at TIMESTAMPTZ NOT NULL
);
