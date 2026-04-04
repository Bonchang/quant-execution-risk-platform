from __future__ import annotations

import pandas as pd

from qerp_research.config import BacktestConfig
from qerp_research.metrics import calculate_metrics
from qerp_research.strategy import build_signals


def run_backtest(market_data: pd.DataFrame, config: BacktestConfig) -> tuple[pd.DataFrame, dict[str, float]]:
    frame = build_signals(market_data, config)
    frame["position"] = frame["target_exposure"].shift(1).fillna(0.0)
    frame["trade"] = frame["position"].diff().fillna(frame["position"])
    cost_rate = (config.transaction_cost_bps + config.slippage_bps) / 10000.0
    frame["cost"] = frame["trade"].abs() * cost_rate
    frame["portfolio_return"] = (frame["position"] * frame["return"]) - frame["cost"]
    frame["equity_curve"] = config.initial_capital * (1.0 + frame["portfolio_return"]).cumprod()
    frame["notional_exposure"] = frame["position"] * frame["close_price"]
    metrics = calculate_metrics(frame)
    return frame, metrics
