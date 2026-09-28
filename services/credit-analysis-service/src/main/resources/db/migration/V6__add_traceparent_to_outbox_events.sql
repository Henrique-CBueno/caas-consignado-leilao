-- Contexto de trace (W3C traceparent) de quem gravou o evento, restaurado pelo relay.
ALTER TABLE outbox_events ADD COLUMN traceparent TEXT;
