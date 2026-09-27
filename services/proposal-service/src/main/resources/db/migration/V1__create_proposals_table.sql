CREATE TABLE proposals (
    id UUID PRIMARY KEY,
    tenant_id UUID NOT NULL,
    borrower_id TEXT NOT NULL,
    requested_amount NUMERIC(15, 2) NOT NULL,
    term_months INTEGER NOT NULL,
    status TEXT NOT NULL
);
