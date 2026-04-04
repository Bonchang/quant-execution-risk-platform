from __future__ import annotations

import pandas as pd
from sqlalchemy import create_engine, text

from qerp_research.config import BacktestConfig


def load_market_data(config: BacktestConfig) -> pd.DataFrame:
    engine = create_engine(config.db.sqlalchemy_url)
    query = text(
        """
        SELECT mp.price_date,
               i.symbol,
               mp.open_price,
               mp.high_price,
               mp.low_price,
               mp.close_price,
               mp.volume
        FROM market_price mp
        JOIN instrument i ON i.id = mp.instrument_id
        WHERE i.symbol = :symbol
        ORDER BY mp.price_date ASC, mp.id ASC
        """
    )
    with engine.begin() as connection:
        frame = pd.read_sql(query, connection, params={"symbol": config.instrument_symbol})
    if frame.empty:
        raise ValueError(f"no market_price rows found for symbol={config.instrument_symbol}")
    frame["price_date"] = pd.to_datetime(frame["price_date"])
    return frame
