from __future__ import annotations

import json
from dataclasses import asdict
from pathlib import Path

import plotly.express as px

from qerp_research.config import BacktestConfig


def write_artifacts(backtest_frame, config: BacktestConfig, metrics: dict[str, float], root: str | Path) -> Path:
    output_dir = Path(root) / config.run_id
    output_dir.mkdir(parents=True, exist_ok=True)

    equity_path = output_dir / "equity_curve.csv"
    trades_path = output_dir / "trades.csv"
    signals_path = output_dir / "signals.csv"
    report_path = output_dir / "report.json"
    chart_path = output_dir / "equity_curve.html"

    backtest_frame[["price_date", "close_price", "position", "equity_curve", "portfolio_return"]].to_csv(equity_path, index=False)
    backtest_frame.loc[backtest_frame["trade"] != 0, ["price_date", "trade", "position", "close_price", "cost"]].to_csv(
        trades_path, index=False
    )
    backtest_frame[
        ["price_date", "fast_ma", "slow_ma", "rolling_vol", "raw_signal", "target_exposure", "position"]
    ].to_csv(signals_path, index=False)

    fig = px.line(backtest_frame, x="price_date", y="equity_curve", title=f"{config.strategy_name} Equity Curve")
    fig.write_html(chart_path)

    report = {
        "runId": config.run_id,
        "strategyName": config.strategy_name,
        "instrumentSymbol": config.instrument_symbol,
        "generatedAt": backtest_frame["price_date"].iloc[-1].isoformat() if not backtest_frame.empty else "",
        "metrics": metrics,
        "config": {
            key: value
            for key, value in asdict(config).items()
            if key != "db"
        },
        "artifactFiles": {
            "equityCurveCsv": str(equity_path),
            "tradesCsv": str(trades_path),
            "signalsCsv": str(signals_path),
            "equityCurveHtml": str(chart_path),
        },
    }
    report_path.write_text(json.dumps(report, indent=2))
    return output_dir
