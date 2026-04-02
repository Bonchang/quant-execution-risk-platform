ALTER TABLE orders
    ADD COLUMN filled_quantity NUMERIC(19, 6) NOT NULL DEFAULT 0,
    ADD COLUMN remaining_quantity NUMERIC(19, 6) NOT NULL DEFAULT 0,
    ADD COLUMN last_executed_at TIMESTAMP,
    ADD COLUMN updated_at TIMESTAMP NOT NULL DEFAULT NOW();

UPDATE orders
SET filled_quantity = CASE
        WHEN status = 'FILLED' THEN quantity
        ELSE 0
    END,
    remaining_quantity = CASE
        WHEN status = 'FILLED' THEN 0
        ELSE quantity
    END,
    last_executed_at = CASE
        WHEN status = 'FILLED' THEN created_at
        ELSE NULL
    END,
    updated_at = created_at;

ALTER TABLE fill
    DROP CONSTRAINT uk_fill_order_id;

CREATE INDEX idx_fill_order_id
    ON fill(order_id);
