# ERD Draft

## Core Entities

### Instrument
금융상품 기본 정보

- id (PK)
- symbol (UNIQUE)
- name
- market

### MarketPrice
종목별 시장 가격 이력

- id (PK)
- instrument_id (FK -> instrument.id)
- price_date
- open_price, high_price, low_price, close_price (NUMERIC)
- volume

### StrategyRun
전략 실행 단위와 파라미터 정보

- id (PK)
- strategy_name
- run_at
- parameters_json

### Order
전략이 생성한 주문 정보

- id (PK)
- strategy_run_id (FK -> strategy_run.id)
- instrument_id (FK -> instrument.id)
- side
- quantity (NUMERIC)
- order_type
- status
- client_order_id
- created_at

Constraints:
- UNIQUE(strategy_run_id, client_order_id)

### RiskCheckResult
리스크 룰 평가 이력

- id (PK)
- order_id (FK -> orders.id)
- rule_name
- passed
- message
- checked_at

### Fill
주문의 체결 결과

- id (PK)
- order_id (FK -> orders.id)
- strategy_run_id (FK -> strategy_run.id)
- instrument_id (FK -> instrument.id)
- fill_quantity (NUMERIC)
- fill_price (NUMERIC)
- filled_at

Constraints:
- UNIQUE(order_id)

### Position
전략 실행 기준 보유 포지션 정보

- id (PK)
- strategy_run_id (FK -> strategy_run.id)
- instrument_id (FK -> instrument.id)
- net_quantity (NUMERIC)
- average_price (NUMERIC)
- updated_at

Constraints:
- UNIQUE(strategy_run_id, instrument_id)

### PortfolioSnapshot (Planned)
일별 포트폴리오 상태 및 성과 스냅샷

- 아직 미구현
