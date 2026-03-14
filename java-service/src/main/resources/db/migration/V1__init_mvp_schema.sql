CREATE TABLE instrument (
    id BIGSERIAL PRIMARY KEY,
    symbol VARCHAR(32) NOT NULL,
    name VARCHAR(128) NOT NULL,
    market VARCHAR(32) NOT NULL,
    CONSTRAINT uk_instrument_symbol UNIQUE (symbol)
);

CREATE TABLE strategy_run (
    id BIGSERIAL PRIMARY KEY,
    strategy_name VARCHAR(128) NOT NULL,
    run_at TIMESTAMP NOT NULL,
    parameters_json TEXT NOT NULL
);

CREATE TABLE market_price (
    id BIGSERIAL PRIMARY KEY,
    instrument_id BIGINT NOT NULL,
    price_date DATE NOT NULL,
    open_price NUMERIC(19, 6) NOT NULL,
    high_price NUMERIC(19, 6) NOT NULL,
    low_price NUMERIC(19, 6) NOT NULL,
    close_price NUMERIC(19, 6) NOT NULL,
    volume BIGINT NOT NULL,
    CONSTRAINT fk_market_price_instrument
        FOREIGN KEY (instrument_id) REFERENCES instrument(id)
);

CREATE TABLE orders (
    id BIGSERIAL PRIMARY KEY,
    strategy_run_id BIGINT NOT NULL,
    instrument_id BIGINT NOT NULL,
    side VARCHAR(16) NOT NULL,
    quantity NUMERIC(19, 6) NOT NULL,
    order_type VARCHAR(16) NOT NULL,
    status VARCHAR(16) NOT NULL,
    client_order_id VARCHAR(64) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    CONSTRAINT fk_orders_strategy_run
        FOREIGN KEY (strategy_run_id) REFERENCES strategy_run(id),
    CONSTRAINT fk_orders_instrument
        FOREIGN KEY (instrument_id) REFERENCES instrument(id),
    CONSTRAINT uk_orders_strategy_run_client_order_id
        UNIQUE (strategy_run_id, client_order_id)
);
