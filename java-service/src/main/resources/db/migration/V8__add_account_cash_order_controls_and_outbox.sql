CREATE TABLE account (
    id BIGSERIAL PRIMARY KEY,
    account_code VARCHAR(64) NOT NULL,
    owner_name VARCHAR(128) NOT NULL,
    base_currency VARCHAR(16) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    CONSTRAINT uk_account_account_code UNIQUE (account_code)
);

CREATE TABLE cash_balance (
    id BIGSERIAL PRIMARY KEY,
    account_id BIGINT NOT NULL,
    available_cash NUMERIC(19, 6) NOT NULL,
    reserved_cash NUMERIC(19, 6) NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    CONSTRAINT fk_cash_balance_account FOREIGN KEY (account_id) REFERENCES account(id),
    CONSTRAINT uk_cash_balance_account UNIQUE (account_id)
);

CREATE TABLE cash_ledger_entry (
    id BIGSERIAL PRIMARY KEY,
    account_id BIGINT NOT NULL,
    order_id BIGINT,
    entry_type VARCHAR(32) NOT NULL,
    amount NUMERIC(19, 6) NOT NULL,
    available_cash_after NUMERIC(19, 6) NOT NULL,
    reserved_cash_after NUMERIC(19, 6) NOT NULL,
    note VARCHAR(255) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    CONSTRAINT fk_cash_ledger_entry_account FOREIGN KEY (account_id) REFERENCES account(id),
    CONSTRAINT fk_cash_ledger_entry_order FOREIGN KEY (order_id) REFERENCES orders(id)
);

INSERT INTO account(account_code, owner_name, base_currency, created_at)
VALUES ('MIGRATION-DEMO', 'Migration Demo Account', 'USD', NOW());

INSERT INTO cash_balance(account_id, available_cash, reserved_cash, updated_at)
SELECT id, 1000000.000000, 0.000000, NOW()
FROM account
WHERE account_code = 'MIGRATION-DEMO';

ALTER TABLE strategy_run
    ADD COLUMN account_id BIGINT;

UPDATE strategy_run
SET account_id = (SELECT id FROM account WHERE account_code = 'MIGRATION-DEMO');

ALTER TABLE strategy_run
    ALTER COLUMN account_id SET NOT NULL,
    ADD CONSTRAINT fk_strategy_run_account FOREIGN KEY (account_id) REFERENCES account(id);

ALTER TABLE orders
    ADD COLUMN account_id BIGINT,
    ADD COLUMN reserved_cash_amount NUMERIC(19, 6) NOT NULL DEFAULT 0,
    ADD COLUMN time_in_force VARCHAR(16) NOT NULL DEFAULT 'DAY',
    ADD COLUMN expires_at TIMESTAMP;

UPDATE orders
SET account_id = (SELECT account_id FROM strategy_run WHERE strategy_run.id = orders.strategy_run_id),
    reserved_cash_amount = 0,
    time_in_force = 'DAY';

ALTER TABLE orders
    ALTER COLUMN account_id SET NOT NULL,
    ADD CONSTRAINT fk_orders_account FOREIGN KEY (account_id) REFERENCES account(id);

ALTER TABLE position
    ADD COLUMN account_id BIGINT,
    ADD CONSTRAINT fk_position_account FOREIGN KEY (account_id) REFERENCES account(id);

UPDATE position
SET account_id = (SELECT account_id FROM strategy_run WHERE strategy_run.id = position.strategy_run_id);

ALTER TABLE position
    ALTER COLUMN account_id SET NOT NULL;

CREATE INDEX idx_position_strategy_run_instrument ON position(strategy_run_id, instrument_id);
CREATE INDEX idx_cash_ledger_entry_account_created_at ON cash_ledger_entry(account_id, created_at DESC, id DESC);
CREATE INDEX idx_orders_account_status ON orders(account_id, status, id DESC);

CREATE TABLE outbox_event (
    id BIGSERIAL PRIMARY KEY,
    aggregate_type VARCHAR(64) NOT NULL,
    aggregate_id BIGINT NOT NULL,
    event_type VARCHAR(64) NOT NULL,
    payload_json TEXT NOT NULL,
    created_at TIMESTAMP NOT NULL,
    processed_at TIMESTAMP,
    processing_status VARCHAR(16) NOT NULL
);

CREATE INDEX idx_outbox_event_processing_status_id ON outbox_event(processing_status, id);
