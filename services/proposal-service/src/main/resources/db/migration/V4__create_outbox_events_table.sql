-- Tabela puramente técnica (nunca consultada por tenant), sem RLS: o relay que
-- publica no Kafka varre eventos de todos os tenants, não faz sentido escopar
-- por app.current_tenant aqui.
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
