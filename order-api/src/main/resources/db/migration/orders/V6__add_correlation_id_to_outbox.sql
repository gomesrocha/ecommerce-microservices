ALTER TABLE orders.outbox_events
    ADD COLUMN IF NOT EXISTS correlation_id VARCHAR(100);

CREATE INDEX IF NOT EXISTS idx_outbox_events_correlation_id
    ON orders.outbox_events(correlation_id);