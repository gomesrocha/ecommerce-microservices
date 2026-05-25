CREATE TABLE IF NOT EXISTS products.stock_reservations (
    id BIGSERIAL PRIMARY KEY,
    order_id BIGINT NOT NULL,
    product_id BIGINT NOT NULL,
    quantity INTEGER NOT NULL,
    status VARCHAR(30) NOT NULL,
    reason VARCHAR(500),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP
);

ALTER TABLE products.stock_reservations
DROP CONSTRAINT IF EXISTS stock_reservations_status_check;

ALTER TABLE products.stock_reservations
ADD CONSTRAINT stock_reservations_status_check
CHECK (status IN ('RESERVED', 'REJECTED'));

CREATE UNIQUE INDEX IF NOT EXISTS uk_stock_reservations_order_product
ON products.stock_reservations(order_id, product_id);

CREATE INDEX IF NOT EXISTS idx_stock_reservations_order_id
ON products.stock_reservations(order_id);

CREATE INDEX IF NOT EXISTS idx_stock_reservations_product_id
ON products.stock_reservations(product_id);
