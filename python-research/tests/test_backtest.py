from __future__ import annotations

import tempfile
import unittest
from pathlib import Path

import pandas as pd

from qerp_research.artifacts import write_artifacts
from qerp_research.backtest import run_backtest
from qerp_research.config import BacktestConfig, DbConfig


class BacktestTest(unittest.TestCase):
    def test_run_backtest_and_write_artifacts(self) -> None:
        config = BacktestConfig(
            run_id="unit-test-run",
            strategy_name="volatility-targeted moving average crossover",
            instrument_symbol="DEMO_AAPL",
            initial_capital=100000.0,
            fast_window=2,
            slow_window=3,
            volatility_window=2,
            target_volatility=0.2,
            max_gross_exposure=1.0,
            transaction_cost_bps=5.0,
            slippage_bps=2.0,
            db=DbConfig(host="localhost", port=5432, name="qerp", user="postgres", password="postgres"),
        )
        market_data = pd.DataFrame(
            {
                "price_date": pd.date_range("2026-01-01", periods=6, freq="D"),
                "symbol": ["DEMO_AAPL"] * 6,
                "open_price": [100, 101, 102, 103, 104, 105],
                "high_price": [101, 102, 103, 104, 105, 106],
                "low_price": [99, 100, 101, 102, 103, 104],
                "close_price": [100, 102, 103, 104, 103, 106],
                "volume": [1000] * 6,
            }
        )

        backtest_frame, metrics = run_backtest(market_data, config)

        self.assertIn("equity_curve", backtest_frame.columns)
        self.assertIn("cagr", metrics)

        with tempfile.TemporaryDirectory() as tmp_dir:
            output_dir = write_artifacts(backtest_frame, config, metrics, Path(tmp_dir))
            self.assertTrue((output_dir / "report.json").exists())
            self.assertTrue((output_dir / "equity_curve.csv").exists())
            self.assertTrue((output_dir / "trades.csv").exists())
            self.assertTrue((output_dir / "signals.csv").exists())


if __name__ == "__main__":
    unittest.main()
