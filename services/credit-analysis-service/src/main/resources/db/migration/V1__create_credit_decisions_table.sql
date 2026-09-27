CREATE TABLE credit_decisions (
    id UUID PRIMARY KEY,
    proposal_id UUID NOT NULL,
    tenant_id UUID NOT NULL,
    decision TEXT NOT NULL,
    confidence DOUBLE PRECISION NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
