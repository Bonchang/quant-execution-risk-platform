from __future__ import annotations

import numpy as np
import pandas as pd

from qerp_research.config import BacktestConfig


def build_signals(market_data: pd.DataFrame, config: BacktestConfig) -> pd.DataFrame:
    frame = market_data.copy()
    frame["return"] = frame["close_price"].pct_change().fillna(0.0)
    frame["fast_ma"] = frame["close_price"].rolling(config.fast_window, min_periods=1).mean()
    frame["slow_ma"] = frame["close_price"].rolling(config.slow_window, min_periods=1).mean()
    frame["rolling_vol"] = (
        frame["return"].rolling(config.volatility_window, min_periods=2).std().fillna(0.0) * np.sqrt(252.0)
    )
    frame["raw_signal"] = np.where(frame["fast_ma"] > frame["slow_ma"], 1.0, 0.0)
    target = np.where(
        frame["rolling_vol"] > 0,
        config.target_volatility / frame["rolling_vol"].replace(0.0, np.nan),
        0.0,
    )
    frame["target_exposure"] = (
        pd.Series(target, index=frame.index)
        .fillna(0.0)
        .clip(lower=0.0, upper=config.max_gross_exposure)
        * frame["raw_signal"]
    )
    return frame
