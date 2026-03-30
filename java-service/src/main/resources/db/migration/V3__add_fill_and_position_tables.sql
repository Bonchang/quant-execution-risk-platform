CREATE TABLE fill (
    id BIGSERIAL PRIMARY KEY,
    order_id BIGINT NOT NULL,
    strategy_run_id BIGINT NOT NULL,
    instrument_id BIGINT NOT NULL,
    fill_quantity NUMERIC(19, 6) NOT NULL,
    fill_price NUMERIC(19, 6) NOT NULL,
    filled_at TIMESTAMP NOT NULL,
    CONSTRAINT fk_fill_order
        FOREIGN KEY (order_id) REFERENCES orders(id),
    CONSTRAINT fk_fill_strategy_run
        FOREIGN KEY (strategy_run_id) REFERENCES strategy_run(id),
    CONSTRAINT fk_fill_instrument
        FOREIGN KEY (instrument_id) REFERENCES instrument(id),
    CONSTRAINT uk_fill_order_id
        UNIQUE (order_id)
);

CREATE INDEX idx_fill_strategy_run_id
    ON fill(strategy_run_id);

CREATE INDEX idx_fill_instrument_id
    ON fill(instrument_id);

CREATE TABLE position (
    id BIGSERIAL PRIMARY KEY,
    strategy_run_id BIGINT NOT NULL,
    instrument_id BIGINT NOT NULL,
    net_quantity NUMERIC(19, 6) NOT NULL,
    average_price NUMERIC(19, 6) NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    CONSTRAINT fk_position_strategy_run
        FOREIGN KEY (strategy_run_id) REFERENCES strategy_run(id),
    CONSTRAINT fk_position_instrument
        FOREIGN KEY (instrument_id) REFERENCES instrument(id),
    CONSTRAINT uk_position_strategy_run_instrument
        UNIQUE (strategy_run_id, instrument_id)
);

CREATE INDEX idx_position_strategy_run_id
    ON position(strategy_run_id);

CREATE INDEX idx_position_instrument_id
    ON position(instrument_id);
