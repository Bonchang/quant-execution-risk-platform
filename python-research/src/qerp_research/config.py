from __future__ import annotations

from dataclasses import dataclass
from pathlib import Path
from typing import Any

import yaml


@dataclass(frozen=True)
class DbConfig:
    host: str
    port: int
    name: str
    user: str
    password: str

    @property
    def sqlalchemy_url(self) -> str:
        return f"postgresql+psycopg://{self.user}:{self.password}@{self.host}:{self.port}/{self.name}"


@dataclass(frozen=True)
class BacktestConfig:
    run_id: str
    strategy_name: str
    instrument_symbol: str
    initial_capital: float
    fast_window: int
    slow_window: int
    volatility_window: int
    target_volatility: float
    max_gross_exposure: float
    transaction_cost_bps: float
    slippage_bps: float
    db: DbConfig


def load_config(path: str | Path) -> BacktestConfig:
    raw: dict[str, Any] = yaml.safe_load(Path(path).read_text())
    return BacktestConfig(
        run_id=raw["run_id"],
        strategy_name=raw["strategy_name"],
        instrument_symbol=raw["instrument_symbol"],
        initial_capital=float(raw["initial_capital"]),
        fast_window=int(raw["fast_window"]),
        slow_window=int(raw["slow_window"]),
        volatility_window=int(raw["volatility_window"]),
        target_volatility=float(raw["target_volatility"]),
        max_gross_exposure=float(raw["max_gross_exposure"]),
        transaction_cost_bps=float(raw["transaction_cost_bps"]),
        slippage_bps=float(raw["slippage_bps"]),
        db=DbConfig(**raw["db"]),
    )
