# System Architecture

## 1. Scope Boundary

QERP는 현재 "전략 주문 생성 -> 리스크 판정 -> 최소 실행 -> 체결/포지션 반영"의 운영 경로를 구현한다.

- 운영 서비스: `java-service`
- 리서치 서비스: `python-research`

운영 트랜잭션은 Java가 담당하고, Python은 실험/연구 워크로드를 담당한다.

## 2. Component Model

### 2.1 Java Service Layering

1. API Layer
- `OrderController`
- `DashboardController`

2. Application Layer
- `OrderService`
- `RiskEvaluationService`
- `OrderExecutionService`
- `DashboardService`

3. Domain/Persistence Layer
- JPA Entities + Spring Data Repositories

4. Infrastructure
- PostgreSQL
- Flyway migrations

### 2.2 Domain Packages

- `instrument`: 종목 마스터
- `market`: 가격 시계열
- `strategyrun`: 전략 실행 컨텍스트
- `order`: 주문 생성/상태
- `risk`: 리스크 룰/판정 결과
- `execution`: 승인 주문 실행
- `fill`: 체결 결과
- `position`: 포지션 상태
- `dashboard`: 진행상황 조회 API

## 3. Order Flow Contract

### 3.1 Input

`POST /orders`

Request fields:
- `strategyRunId`
- `instrumentId`
- `side` (`BUY|SELL`)
- `quantity`
- `orderType` (`MARKET|LIMIT`)
- `clientOrderId`

### 3.2 State Machine

- Initial: `CREATED`
- Risk pass: `APPROVED`
- Risk fail: `REJECTED`
- Approved execution done: `FILLED`

### 3.3 Persistence Side Effects

- always: `orders`
- always: `risk_check_result` (rule-by-rule)
- approved only: `fill`
- approved only: `position`

## 4. Risk Engine Design (Current)

### 4.1 Abstraction

- `RiskRule` interface
- `RiskRuleResult` value model

### 4.2 Implemented Rules

1. `MAX_ORDER_QUANTITY`
- 단일 주문 수량 상한

2. `INSTRUMENT_QUANTITY_EXPOSURE`
- 종목별 누적 노출 상한
- `APPROVED` + `FILLED` 주문 수량을 기준으로 계산

### 4.3 Auditability

각 룰 실행 결과는 `risk_check_result`에 저장된다.
- `rule_name`
- `passed`
- `message`
- `checked_at`

## 5. Execution Model (Current MVP)

- `APPROVED` 주문만 실행
- 현재는 단순 즉시체결 모델
- 체결 가격
  - 우선: 최신 `market_price.close_price`
  - fallback: `execution.default-fill-price`

### Position Update Logic

- BUY: 가중평균 단가로 `net_quantity`, `average_price` 갱신
- SELL: `net_quantity` 감소, 수량 0 이하 시 평균단가 0으로 리셋

## 6. Schema and Migration Strategy

Flyway versions:
- `V1`: instrument/market_price/strategy_run/orders
- `V2`: risk_check_result
- `V3`: fill/position

운영 원칙:
- Schema evolution은 Flyway 전용
- JPA는 validate 모드

## 7. Frontend Progress Dashboard

정적 웹 UI(`/`)는 운영 검증 도구 역할을 수행한다.

Features:
- 주문 생성 폼
- 상태 집계 카드
- 최근 주문/리스크/체결/포지션 테이블
- 자동 새로고침

Data source:
- `/dashboard/overview`

## 8. Architectural Next Steps

1. `PortfolioSnapshot` 도입
2. execution policy 확장(부분체결/슬리피지/수수료)
3. 리스크 룰 확장(현금/계좌/포지션 기반)
4. 운영 관측성(메트릭, alert, tracing)
