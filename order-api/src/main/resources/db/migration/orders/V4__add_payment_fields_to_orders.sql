ALTER TABLE orders.customer_orders
    ADD COLUMN IF NOT EXISTS payment_status VARCHAR(50),
    ADD COLUMN IF NOT EXISTS payment_transaction_id VARCHAR(100),
    ADD COLUMN IF NOT EXISTS payment_authorization_code VARCHAR(100),
    ADD COLUMN IF NOT EXISTS payment_reason TEXT;