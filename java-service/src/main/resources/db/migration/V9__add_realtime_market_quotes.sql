CREATE TABLE market_quote (
    id BIGSERIAL PRIMARY KEY,
    instrument_id BIGINT NOT NULL,
    quote_time TIMESTAMP NOT NULL,
    last_price NUMERIC(19, 6) NOT NULL,
    bid_price NUMERIC(19, 6) NOT NULL,
    ask_price NUMERIC(19, 6) NOT NULL,
    change_percent NUMERIC(19, 6) NOT NULL,
    source VARCHAR(32) NOT NULL,
    received_at TIMESTAMP NOT NULL,
    CONSTRAINT fk_market_quote_instrument FOREIGN KEY (instrument_id) REFERENCES instrument(id),
    CONSTRAINT uk_market_quote_instrument UNIQUE (instrument_id)
);

CREATE INDEX idx_market_quote_received_at ON market_quote(received_at DESC, id DESC);
CREATE INDEX idx_market_quote_quote_time ON market_quote(quote_time DESC, id DESC);

CREATE TABLE market_data_run (
    id BIGSERIAL PRIMARY KEY,
    started_at TIMESTAMP NOT NULL,
    finished_at TIMESTAMP NOT NULL,
    source VARCHAR(32) NOT NULL,
    total_instruments INT NOT NULL,
    success_count INT NOT NULL,
    failure_count INT NOT NULL,
    run_status VARCHAR(16) NOT NULL,
    last_quote_received_at TIMESTAMP,
    updated_symbols_json TEXT NOT NULL,
    failure_messages_json TEXT NOT NULL,
    stale_instruments_json TEXT NOT NULL
);

CREATE INDEX idx_market_data_run_finished_at ON market_data_run(finished_at DESC, id DESC);
