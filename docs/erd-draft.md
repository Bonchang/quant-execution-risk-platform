# ERD Draft

## Core Entities

### Instrument

금융상품 기본 정보

- `id` (PK)
- `symbol` (UNIQUE)
- `name`
- `market`

### MarketPrice

종목별 시장 가격 이력

- `id` (PK)
- `instrument_id` (FK -> `instrument.id`)
- `price_date`
- `open_price`
- `high_price`
- `low_price`
- `close_price`
- `volume`

Constraints:

- UNIQUE(`instrument_id`, `price_date`)

### StrategyRun

전략 실행 단위와 파라미터 정보

- `id` (PK)
- `strategy_name`
- `run_at`
- `parameters_json`

### Order

전략이 생성한 주문 정보

- `id` (PK)
- `strategy_run_id` (FK -> `strategy_run.id`)
- `instrument_id` (FK -> `instrument.id`)
- `side`
- `quantity` (NUMERIC)
- `filled_quantity` (NUMERIC)
- `remaining_quantity` (NUMERIC)
- `order_type`
- `limit_price` (NUMERIC, nullable)
- `status`
- `client_order_id`
- `created_at`
- `last_executed_at` (nullable)
- `updated_at`

Constraints:

- UNIQUE(`strategy_run_id`, `client_order_id`)

### RiskCheckResult

리스크 룰 평가 이력

- `id` (PK)
- `order_id` (FK -> `orders.id`)
- `rule_name`
- `passed`
- `message`
- `checked_at`

### Fill

주문의 체결 결과

- `id` (PK)
- `order_id` (FK -> `orders.id`)
- `strategy_run_id` (FK -> `strategy_run.id`)
- `instrument_id` (FK -> `instrument.id`)
- `fill_quantity` (NUMERIC)
- `fill_price` (NUMERIC)
- `filled_at`

Notes:

- 현재는 주문당 다중 fill 허용
- `order_id`는 index 대상이지만 unique 제약은 없음

### Position

전략 실행 기준 보유 포지션 정보

- `id` (PK)
- `strategy_run_id` (FK -> `strategy_run.id`)
- `instrument_id` (FK -> `instrument.id`)
- `net_quantity` (NUMERIC)
- `average_price` (NUMERIC)
- `updated_at`

Constraints:

- UNIQUE(`strategy_run_id`, `instrument_id`)

### PortfolioSnapshot

전략 실행 기준 포트폴리오 상태 스냅샷

- `id` (PK)
- `strategy_run_id` (FK -> `strategy_run.id`)
- `snapshot_at`
- `total_market_value` (NUMERIC)
- `unrealized_pnl` (NUMERIC)
- `realized_pnl` (NUMERIC)
- `total_pnl` (NUMERIC)
- `return_rate` (NUMERIC)

## Relationship Summary

- `strategy_run` 1:N `orders`
- `instrument` 1:N `orders`
- `orders` 1:N `risk_check_result`
- `orders` 1:N `fill`
- `strategy_run` 1:N `fill`
- `instrument` 1:N `fill`
- `strategy_run` 1:N `portfolio_snapshot`
- `strategy_run` 1:N `position`
- `instrument` 1:N `position`
- `instrument` 1:N `market_price`
