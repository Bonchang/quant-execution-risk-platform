# System Architecture

## 1. Scope Boundary

QERP는 현재 아래 운영 경로를 구현한다.

- 전략 실행 컨텍스트 생성
- 주문 생성
- 룰 기반 리스크 판정
- 승인 주문 실행
- 체결/포지션 반영
- 포트폴리오 스냅샷 생성
- 대시보드/시장데이터 상태 조회

구성 경계:

- 운영 서비스: `java-service`
- 리서치 공간: `python-research`

운영 트랜잭션은 Java 서비스가 담당하고, Python 영역은 아직 placeholder 상태다.

## 2. Component Model

### 2.1 Java Service Layering

1. API Layer
- `OrderController`
- `DashboardController`
- `MarketDataController`

2. Application Layer
- `OrderService`
- `RiskEvaluationService`
- `OrderExecutionService`
- `PortfolioSnapshotService`
- `DashboardService`
- `MarketDataIngestionService`
- `MarketDataStatusService`

3. Domain/Persistence Layer
- JPA Entities
- Spring Data Repositories

4. Infrastructure
- PostgreSQL
- Flyway
- Scheduler
- Finnhub HTTP client

### 2.2 Domain Packages

- `instrument`: 종목 마스터
- `market`: 가격 시계열
- `marketdata`: 외부 시세 수집 및 상태
- `strategyrun`: 전략 실행 컨텍스트
- `order`: 주문 생성/상태
- `risk`: 리스크 룰/판정 결과
- `execution`: 승인 주문 실행
- `fill`: 체결 결과
- `position`: 포지션 상태
- `portfolio`: 포트폴리오 스냅샷 및 PnL 계산
- `dashboard`: 운영 집계/옵션/데모 API

## 3. Order Flow Contract

### 3.1 Input

`POST /orders`

Request fields:

- `strategyRunId`
- `instrumentId`
- `side` (`BUY|SELL`)
- `quantity`
- `orderType` (`MARKET|LIMIT`)
- `limitPrice` (`LIMIT`일 때 필수)
- `clientOrderId`

### 3.2 State Machine

- Initial: `CREATED`
- Risk pass: `APPROVED`
- Risk fail: `REJECTED`
- Approved but unfilled LIMIT: `APPROVED`
- Partial execution: `PARTIALLY_FILLED`
- Full execution: `FILLED`

### 3.3 Persistence Side Effects

- always: `orders`
- always: `risk_check_result`
- approved and executable: `fill`
- approved and executable: `position`
- any fill occurs: `portfolio_snapshot`

## 4. Risk Engine Design

### 4.1 Abstraction

- `RiskRule` interface
- `RiskRuleResult` value model
- `RiskEvaluationService`가 등록된 룰을 순차 실행

### 4.2 Implemented Rules

1. `MAX_ORDER_QUANTITY`
- 단일 주문 수량 상한

2. `INSTRUMENT_QUANTITY_EXPOSURE`
- 종목별 signed quantity exposure 상한
- BUY는 노출 증가, SELL은 노출 감소로 계산
- 기준 상태: `APPROVED`, `PARTIALLY_FILLED`, `FILLED`

### 4.3 Auditability

각 룰 실행 결과는 `risk_check_result`에 저장된다.

- `rule_name`
- `passed`
- `message`
- `checked_at`

## 5. Execution Model

### 5.1 MARKET Orders

- `APPROVED` 직후 즉시 실행
- 최신 `market_price.close_price` 우선 사용
- 가격이 없으면 `execution.default-fill-price` 사용
- 데모 목적상 2개 fill 청크로 분할 저장
- 최종 상태는 `FILLED`

### 5.2 LIMIT Orders

- `limit_price` 필수
- 기준 가격은 최신 `market_price.close_price`
- BUY LIMIT: `close_price <= limit_price`
- SELL LIMIT: `close_price >= limit_price`
- 조건 불충족 시 fill 없이 `APPROVED` 유지
- 조건 충족 시 1회 fill 후 `FILLED`

### 5.3 Position Update Logic

- BUY
  - `net_quantity` 증가
  - 가중평균단가로 `average_price` 재계산
- SELL
  - `net_quantity` 감소
  - `net_quantity <= 0`이면 현재 구현상 `average_price`를 0으로 초기화

## 6. Portfolio Analytics Model

- 데이터 소스
  - `position`
  - 최신 `market_price`
  - `fill` history
- 산출 지표
  - `total_market_value`
  - `unrealized_pnl`
  - `realized_pnl`
  - `total_pnl`
  - `return_rate`
- 생성 방식
  - fill 발생 시 자동 생성
  - `/dashboard/portfolio-snapshots/refresh` 수동 생성

## 7. Market Data Ingestion

- `POST /market-data/ingest`
- `GET /market-data/status`
- `market-data.enabled=true`일 때 스케줄러 동작
- `FINNHUB_API_KEY` 미설정 시 graceful degradation
- 동일 `(instrument_id, price_date)`는 upsert 형태로 유지

## 8. Schema and Migration Strategy

Flyway versions:

- `V1`: instrument / market_price / strategy_run / orders
- `V2`: risk_check_result
- `V3`: fill / position
- `V4`: market price uniqueness
- `V5`: order execution lifecycle fields + multi-fill support
- `V6`: `limit_price`
- `V7`: portfolio_snapshot

운영 원칙:

- schema evolution은 Flyway 전용
- JPA는 `validate` 모드

## 9. Dashboard

정적 웹 UI(`/`)는 운영 검증 콘솔 역할을 수행한다.

기능:

- 주문 생성
- 전략/종목 옵션 조회
- 데모 데이터 생성
- 상태 집계 카드
- 최근 주문/리스크/체결/포지션/포트폴리오 스냅샷 조회
- 시장데이터 상태/수동 수집

주요 데이터 소스:

- `/dashboard/overview`
- `/dashboard/options`
- `/market-data/status`

## 10. Architectural Next Steps

1. 현금/계좌 기반 리스크 도입
2. 동시성 안전한 승인/노출 계산 강화
3. 고급 실행 정책(부분체결, 슬리피지, 수수료)
4. 숏 포지션 및 고급 PnL 정책 정교화
5. 인증/권한 및 운영 관측성 보강
