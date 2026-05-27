CREATE SCHEMA IF NOT EXISTS notifications;

CREATE TABLE IF NOT EXISTS notifications.notifications (
    id BIGSERIAL PRIMARY KEY,
    event_id VARCHAR(100),
    event_type VARCHAR(100) NOT NULL,
    aggregate_type VARCHAR(100),
    aggregate_id BIGINT,
    user_id BIGINT,
    title VARCHAR(255) NOT NULL,
    message TEXT NOT NULL,
    severity VARCHAR(30) NOT NULL,
    status VARCHAR(30) NOT NULL,
    metadata_json TEXT,
    created_at TIMESTAMP NOT NULL,
    read_at TIMESTAMP
);

CREATE TABLE IF NOT EXISTS notifications.notification_deliveries (
    id BIGSERIAL PRIMARY KEY,
    notification_id BIGINT NOT NULL REFERENCES notifications.notifications(id),
    channel VARCHAR(30) NOT NULL,
    destination VARCHAR(255),
    status VARCHAR(30) NOT NULL,
    attempts INTEGER NOT NULL DEFAULT 0,
    last_error TEXT,
    created_at TIMESTAMP NOT NULL,
    sent_at TIMESTAMP,
    updated_at TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_notifications_user_id
    ON notifications.notifications(user_id);

CREATE INDEX IF NOT EXISTS idx_notifications_status
    ON notifications.notifications(status);

CREATE INDEX IF NOT EXISTS idx_notification_deliveries_notification_id
    ON notifications.notification_deliveries(notification_id);

CREATE INDEX IF NOT EXISTS idx_notification_deliveries_channel
    ON notifications.notification_deliveries(channel);