-- Tabela puramente técnica (nunca consultada por tenant), sem RLS — ver
-- migration equivalente do proposal-service para o racional completo.
CREATE TABLE outbox_events (
    id UUID PRIMARY KEY,
    aggregate_type TEXT NOT NULL,
    aggregate_id UUID NOT NULL,
    event_type TEXT NOT NULL,
    payload TEXT NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    published_at TIMESTAMPTZ
);

GRANT SELECT, INSERT, UPDATE ON outbox_events TO app_role;
