CREATE TABLE IF NOT EXISTS orders.outbox_events (
    id BIGSERIAL PRIMARY KEY,
    event_id UUID NOT NULL,
    aggregate_type VARCHAR(80) NOT NULL,
    aggregate_id BIGINT NOT NULL,
    event_type VARCHAR(120) NOT NULL,
    routing_key VARCHAR(120) NOT NULL,
    payload TEXT NOT NULL,
    status VARCHAR(30) NOT NULL,
    attempts INTEGER NOT NULL DEFAULT 0,
    last_error VARCHAR(1000),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP,
    published_at TIMESTAMP
);

ALTER TABLE orders.outbox_events
DROP CONSTRAINT IF EXISTS outbox_events_status_check;

ALTER TABLE orders.outbox_events
ADD CONSTRAINT outbox_events_status_check
CHECK (status IN ('PENDING', 'PUBLISHED', 'FAILED'));

CREATE INDEX IF NOT EXISTS idx_outbox_events_status_created_at
ON orders.outbox_events(status, created_at);

CREATE INDEX IF NOT EXISTS idx_outbox_events_event_id
ON orders.outbox_events(event_id);
