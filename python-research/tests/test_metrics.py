from __future__ import annotations

import unittest

import pandas as pd

from qerp_research.metrics import calculate_metrics


class MetricsTest(unittest.TestCase):
    def test_calculate_metrics_returns_expected_keys(self) -> None:
        frame = pd.DataFrame(
            {
                "portfolio_return": [0.01, -0.005, 0.02, 0.0],
                "trade": [0.5, 0.0, -0.5, 0.0],
            }
        )
        metrics = calculate_metrics(frame)
        self.assertEqual(set(metrics.keys()), {"cagr", "sharpe", "max_drawdown", "turnover", "win_rate"})
        self.assertGreater(metrics["turnover"], 0)


if __name__ == "__main__":
    unittest.main()
