ALTER TABLE orders.customer_orders
DROP CONSTRAINT IF EXISTS customer_orders_status_check;

ALTER TABLE orders.customer_orders
ADD CONSTRAINT customer_orders_status_check
CHECK (status IN ('CREATED', 'WAITING_STOCK', 'WAITING_FRAUD', 'CONFIRMED', 'CANCELED', 'REJECTED'));
