CREATE SCHEMA IF NOT EXISTS orders;

CREATE TABLE IF NOT EXISTS orders.customer_orders (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    customer_state VARCHAR(2) NOT NULL,
    status VARCHAR(30) NOT NULL,
    total_amount NUMERIC(12, 2) NOT NULL,
    min_delivery_days INTEGER,
    estimated_delivery_days INTEGER,
    max_delivery_days INTEGER,
    delivery_source VARCHAR(50),
    delivery_model_version VARCHAR(80),
    fraud_risk_score NUMERIC(5, 2),
    fraud_reason VARCHAR(500),
    stock_reason VARCHAR(500),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP
);

ALTER TABLE orders.customer_orders
ADD COLUMN IF NOT EXISTS user_id BIGINT;

ALTER TABLE orders.customer_orders
ADD COLUMN IF NOT EXISTS customer_state VARCHAR(2);

ALTER TABLE orders.customer_orders
ADD COLUMN IF NOT EXISTS status VARCHAR(30);

ALTER TABLE orders.customer_orders
ADD COLUMN IF NOT EXISTS total_amount NUMERIC(12, 2);

ALTER TABLE orders.customer_orders
ADD COLUMN IF NOT EXISTS min_delivery_days INTEGER;

ALTER TABLE orders.customer_orders
ADD COLUMN IF NOT EXISTS estimated_delivery_days INTEGER;

ALTER TABLE orders.customer_orders
ADD COLUMN IF NOT EXISTS max_delivery_days INTEGER;

ALTER TABLE orders.customer_orders
ADD COLUMN IF NOT EXISTS delivery_source VARCHAR(50);

ALTER TABLE orders.customer_orders
ADD COLUMN IF NOT EXISTS delivery_model_version VARCHAR(80);

ALTER TABLE orders.customer_orders
ADD COLUMN IF NOT EXISTS fraud_risk_score NUMERIC(5, 2);

ALTER TABLE orders.customer_orders
ADD COLUMN IF NOT EXISTS fraud_reason VARCHAR(500);

ALTER TABLE orders.customer_orders
ADD COLUMN IF NOT EXISTS stock_reason VARCHAR(500);

ALTER TABLE orders.customer_orders
ADD COLUMN IF NOT EXISTS created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP;

ALTER TABLE orders.customer_orders
ADD COLUMN IF NOT EXISTS updated_at TIMESTAMP;

ALTER TABLE orders.customer_orders
DROP CONSTRAINT IF EXISTS customer_orders_status_check;

ALTER TABLE orders.customer_orders
ADD CONSTRAINT customer_orders_status_check
CHECK (status IN ('CREATED', 'WAITING_STOCK', 'WAITING_FRAUD', 'CONFIRMED', 'CANCELED', 'REJECTED'));

CREATE TABLE IF NOT EXISTS orders.order_items (
    id BIGSERIAL PRIMARY KEY,
    order_id BIGINT NOT NULL,
    product_id BIGINT NOT NULL,
    product_name VARCHAR(120) NOT NULL,
    product_sku VARCHAR(80) NOT NULL,
    quantity INTEGER NOT NULL,
    unit_price NUMERIC(10, 2) NOT NULL,
    total_price NUMERIC(12, 2) NOT NULL,
    origin_state VARCHAR(2) NOT NULL
);

ALTER TABLE orders.order_items
ADD COLUMN IF NOT EXISTS order_id BIGINT;

ALTER TABLE orders.order_items
ADD COLUMN IF NOT EXISTS product_id BIGINT;

ALTER TABLE orders.order_items
ADD COLUMN IF NOT EXISTS product_name VARCHAR(120);

ALTER TABLE orders.order_items
ADD COLUMN IF NOT EXISTS product_sku VARCHAR(80);

ALTER TABLE orders.order_items
ADD COLUMN IF NOT EXISTS quantity INTEGER;

ALTER TABLE orders.order_items
ADD COLUMN IF NOT EXISTS unit_price NUMERIC(10, 2);

ALTER TABLE orders.order_items
ADD COLUMN IF NOT EXISTS total_price NUMERIC(12, 2);

ALTER TABLE orders.order_items
ADD COLUMN IF NOT EXISTS origin_state VARCHAR(2);

CREATE INDEX IF NOT EXISTS idx_order_items_order_id
ON orders.order_items(order_id);

CREATE TABLE IF NOT EXISTS orders.order_status_history (
    id BIGSERIAL PRIMARY KEY,
    order_id BIGINT NOT NULL,
    previous_status VARCHAR(30),
    new_status VARCHAR(30) NOT NULL,
    trigger_event VARCHAR(80) NOT NULL,
    reason VARCHAR(500),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_order_status_history_order_id
ON orders.order_status_history(order_id);
