DELETE FROM market_price a
USING market_price b
WHERE a.id < b.id
  AND a.instrument_id = b.instrument_id
  AND a.price_date = b.price_date;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM pg_constraint
        WHERE conname = 'uk_market_price_instrument_price_date'
    ) THEN
        ALTER TABLE market_price
            ADD CONSTRAINT uk_market_price_instrument_price_date
                UNIQUE (instrument_id, price_date);
    END IF;
END
$$;
