CREATE TABLE app_user (
    id BIGSERIAL PRIMARY KEY,
    account_id BIGINT,
    auth_type VARCHAR(16) NOT NULL,
    display_name VARCHAR(128) NOT NULL,
    email VARCHAR(255),
    password_hash VARCHAR(255),
    role VARCHAR(32) NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL,
    last_login_at TIMESTAMP NOT NULL,
    CONSTRAINT fk_app_user_account FOREIGN KEY (account_id) REFERENCES account(id),
    CONSTRAINT uk_app_user_email UNIQUE (email)
);

CREATE INDEX idx_app_user_auth_type_last_login_at
    ON app_user(auth_type, last_login_at DESC, id DESC);

INSERT INTO instrument(symbol, name, market)
VALUES
    ('AAPL', 'Apple Inc.', 'NASDAQ'),
    ('MSFT', 'Microsoft Corporation', 'NASDAQ'),
    ('NVDA', 'NVIDIA Corporation', 'NASDAQ'),
    ('AMZN', 'Amazon.com, Inc.', 'NASDAQ'),
    ('GOOGL', 'Alphabet Inc.', 'NASDAQ'),
    ('META', 'Meta Platforms, Inc.', 'NASDAQ'),
    ('TSLA', 'Tesla, Inc.', 'NASDAQ'),
    ('NFLX', 'Netflix, Inc.', 'NASDAQ'),
    ('AMD', 'Advanced Micro Devices, Inc.', 'NASDAQ'),
    ('CRM', 'Salesforce, Inc.', 'NYSE'),
    ('JPM', 'JPMorgan Chase & Co.', 'NYSE'),
    ('KO', 'The Coca-Cola Company', 'NYSE')
ON CONFLICT (symbol) DO UPDATE
    SET name = EXCLUDED.name,
        market = EXCLUDED.market;

WITH base(symbol, base_close, drift, volume_base) AS (
    VALUES
        ('AAPL', 189.500000::NUMERIC, 0.35::NUMERIC, 1450000::BIGINT),
        ('MSFT', 421.250000::NUMERIC, 0.42::NUMERIC, 1380000::BIGINT),
        ('NVDA', 927.800000::NUMERIC, 1.55::NUMERIC, 1710000::BIGINT),
        ('AMZN', 181.750000::NUMERIC, 0.28::NUMERIC, 1290000::BIGINT),
        ('GOOGL', 163.450000::NUMERIC, 0.24::NUMERIC, 1260000::BIGINT),
        ('META', 507.300000::NUMERIC, 0.61::NUMERIC, 1180000::BIGINT),
        ('TSLA', 172.200000::NUMERIC, 0.58::NUMERIC, 1830000::BIGINT),
        ('NFLX', 635.400000::NUMERIC, 0.49::NUMERIC, 1010000::BIGINT),
        ('AMD', 171.900000::NUMERIC, 0.44::NUMERIC, 1530000::BIGINT),
        ('CRM', 302.550000::NUMERIC, 0.29::NUMERIC, 940000::BIGINT),
        ('JPM', 198.450000::NUMERIC, 0.18::NUMERIC, 990000::BIGINT),
        ('KO', 62.300000::NUMERIC, 0.05::NUMERIC, 870000::BIGINT)
),
series AS (
    SELECT
        i.id AS instrument_id,
        (CURRENT_DATE - (29 - gs.day)) AS price_date,
        round((b.base_close + (gs.day * b.drift) + (((gs.day % 5) - 2) * 0.37))::NUMERIC, 6) AS close_price,
        round((b.base_close + (gs.day * b.drift) + (((gs.day % 5) - 2) * 0.22))::NUMERIC, 6) AS open_price,
        round((b.base_close + (gs.day * b.drift) + 1.950000 + ((gs.day % 3) * 0.28))::NUMERIC, 6) AS high_price,
        round((b.base_close + (gs.day * b.drift) - 1.850000 - ((gs.day % 3) * 0.24))::NUMERIC, 6) AS low_price,
        (b.volume_base + (gs.day * 2500))::BIGINT AS volume
    FROM base b
    JOIN instrument i ON i.symbol = b.symbol
    CROSS JOIN generate_series(0, 29) AS gs(day)
)
INSERT INTO market_price(instrument_id, price_date, open_price, high_price, low_price, close_price, volume)
SELECT instrument_id, price_date, open_price, high_price, low_price, close_price, volume
FROM series
ON CONFLICT (instrument_id, price_date) DO NOTHING;

WITH latest_price AS (
    SELECT DISTINCT ON (mp.instrument_id)
        mp.instrument_id,
        mp.close_price
    FROM market_price mp
    ORDER BY mp.instrument_id, mp.price_date DESC, mp.id DESC
)
INSERT INTO market_quote(instrument_id, quote_time, last_price, bid_price, ask_price, change_percent, source, received_at)
SELECT
    lp.instrument_id,
    NOW(),
    lp.close_price,
    round((lp.close_price * 0.9995)::NUMERIC, 6),
    round((lp.close_price * 1.0005)::NUMERIC, 6),
    round((((lp.close_price / NULLIF((lp.close_price - 1.15), 0)) - 1) * 100)::NUMERIC, 6),
    'PUBLIC_DEMO_SEED',
    NOW()
FROM latest_price lp
ON CONFLICT (instrument_id) DO UPDATE
    SET quote_time = EXCLUDED.quote_time,
        last_price = EXCLUDED.last_price,
        bid_price = EXCLUDED.bid_price,
        ask_price = EXCLUDED.ask_price,
        change_percent = EXCLUDED.change_percent,
        source = EXCLUDED.source,
        received_at = EXCLUDED.received_at;
