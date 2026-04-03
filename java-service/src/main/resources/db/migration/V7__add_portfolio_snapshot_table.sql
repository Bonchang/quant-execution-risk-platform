CREATE TABLE portfolio_snapshot (
    id BIGSERIAL PRIMARY KEY,
    strategy_run_id BIGINT NOT NULL,
    snapshot_at TIMESTAMP NOT NULL,
    total_market_value NUMERIC(19, 6) NOT NULL,
    unrealized_pnl NUMERIC(19, 6) NOT NULL,
    realized_pnl NUMERIC(19, 6) NOT NULL,
    total_pnl NUMERIC(19, 6) NOT NULL,
    return_rate NUMERIC(19, 6) NOT NULL,
    CONSTRAINT fk_portfolio_snapshot_strategy_run
        FOREIGN KEY (strategy_run_id) REFERENCES strategy_run(id)
);

CREATE INDEX idx_portfolio_snapshot_strategy_run_id
    ON portfolio_snapshot(strategy_run_id);

CREATE INDEX idx_portfolio_snapshot_snapshot_at
    ON portfolio_snapshot(snapshot_at DESC, id DESC);
