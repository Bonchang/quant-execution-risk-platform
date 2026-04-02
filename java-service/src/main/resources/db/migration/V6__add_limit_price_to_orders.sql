ALTER TABLE orders
    ADD COLUMN limit_price NUMERIC(19, 6);

UPDATE orders
SET limit_price = NULL
WHERE order_type = 'MARKET';
