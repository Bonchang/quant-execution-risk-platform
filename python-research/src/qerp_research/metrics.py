from __future__ import annotations

import math

import pandas as pd


def calculate_metrics(backtest_frame: pd.DataFrame) -> dict[str, float]:
    portfolio_return = backtest_frame["portfolio_return"].fillna(0.0)
    trade_return = backtest_frame.loc[backtest_frame["trade"] != 0, "portfolio_return"]
    cumulative = (1.0 + portfolio_return).cumprod()
    periods = max(len(backtest_frame), 1)
    ending = float(cumulative.iloc[-1]) if not cumulative.empty else 1.0
    cagr = ending ** (252.0 / periods) - 1.0 if periods > 0 else 0.0
    std = float(portfolio_return.std(ddof=0))
    sharpe = (float(portfolio_return.mean()) / std * math.sqrt(252.0)) if std > 0 else 0.0
    peak = cumulative.cummax()
    drawdown = (cumulative / peak) - 1.0
    turnover = float(backtest_frame["trade"].abs().sum())
    win_rate = float((trade_return > 0).mean()) if not trade_return.empty else 0.0
    return {
        "cagr": round(cagr, 6),
        "sharpe": round(sharpe, 6),
        "max_drawdown": round(float(drawdown.min()) if not drawdown.empty else 0.0, 6),
        "turnover": round(turnover, 6),
        "win_rate": round(win_rate, 6),
    }
