from __future__ import annotations

import argparse
from pathlib import Path

from qerp_research.artifacts import write_artifacts
from qerp_research.backtest import run_backtest
from qerp_research.config import load_config
from qerp_research.data import load_market_data


def main() -> None:
    parser = argparse.ArgumentParser(description="Run the QERP research backtest and write artifacts.")
    parser.add_argument("--config", default="configs/demo_strategy.yaml", help="Path to YAML config")
    parser.add_argument("--artifacts-dir", default="artifacts", help="Directory to write output artifacts")
    args = parser.parse_args()

    config = load_config(args.config)
    market_data = load_market_data(config)
    backtest_frame, metrics = run_backtest(market_data, config)
    output_dir = write_artifacts(backtest_frame, config, metrics, Path(args.artifacts_dir))
    print(f"artifacts written to {output_dir}")


if __name__ == "__main__":
    main()
