CREATE SCHEMA IF NOT EXISTS payments;

CREATE TABLE IF NOT EXISTS payments.payments (
    id BIGSERIAL PRIMARY KEY,
    event_id VARCHAR(100),
    order_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    amount NUMERIC(15, 2) NOT NULL,
    currency VARCHAR(10) NOT NULL,
    payment_method VARCHAR(50) NOT NULL,
    payment_token VARCHAR(255),
    installments INTEGER NOT NULL DEFAULT 1,
    provider VARCHAR(100),
    provider_transaction_id VARCHAR(100),
    authorization_code VARCHAR(100),
    status VARCHAR(50) NOT NULL,
    reason TEXT,
    attempts INTEGER NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP,
    approved_at TIMESTAMP,
    rejected_at TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_payments_order_id
    ON payments.payments(order_id);

CREATE INDEX IF NOT EXISTS idx_payments_user_id
    ON payments.payments(user_id);

CREATE INDEX IF NOT EXISTS idx_payments_status
    ON payments.payments(status);