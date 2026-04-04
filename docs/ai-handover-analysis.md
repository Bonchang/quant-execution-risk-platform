# QERP 프로젝트 종합 분석 문서 (AI 핸드오버용)

> 대상 리포지토리: `quant-execution-risk-platform_V1`  
> 목적: 다른 AI/엔지니어가 현재 구현 범위, 제약, 다음 개선 우선순위를 빠르게 파악하도록 돕는 기준 문서

## 1. 한 줄 요약

QERP는 전략 주문을 생성하고, 룰 기반 리스크를 평가한 뒤, 승인 주문을 MARKET 또는 LIMIT 정책으로 실행해 체결, 포지션, 포트폴리오 스냅샷까지 적재하는 Spring Boot 기반 MVP다.

## 2. 현재 구현 상태

### 2.1 구현 완료

- Spring Boot + PostgreSQL + Flyway 실행환경
- 핵심 도메인
  - `instrument`
  - `market_price`
  - `strategy_run`
  - `orders`
  - `risk_check_result`
  - `fill`
  - `position`
  - `portfolio_snapshot`
- 주문 API
  - `POST /orders`
- 대시보드 API
  - `GET /dashboard/overview`
  - `GET /dashboard/options`
  - `POST /dashboard/seed-demo`
  - `POST /dashboard/portfolio-snapshots/refresh`
- 시장데이터 API
  - `POST /market-data/ingest`
  - `GET /market-data/status`
- 통합 테스트 + PnL 계산 단위 테스트

### 2.2 아직 미구현

- 현금/계좌 기반 리스크
- 실거래 브로커 연동
- 인증/권한
- 메시지 브로커 기반 비동기 처리
- 숏 포지션 회계 정책 정교화

## 3. 핵심 플로우

1. `POST /orders`
2. `Order(status=CREATED)` 저장
3. `RiskRule` 전체 평가
4. `risk_check_result` 룰별 저장
5. 전체 통과 시 `APPROVED`, 실패 시 `REJECTED`
6. `APPROVED` 주문은 실행 서비스로 전달
7. 실행 시 `fill`, `position`, `portfolio_snapshot` 반영
8. 최종 상태 반환

## 4. 실행 정책

### 4.1 MARKET

- 승인 즉시 실행
- 최신 종가 우선, 없으면 기본 체결가 사용
- 2개 fill 청크로 분할 저장
- 최종 상태 `FILLED`

### 4.2 LIMIT

- `limit_price` 필수
- BUY: 최신 종가가 한도 이하일 때 체결
- SELL: 최신 종가가 한도 이상일 때 체결
- 미체결이면 `APPROVED` 유지

## 5. 리스크 엔진 요약

구현된 룰:

1. `MAX_ORDER_QUANTITY`
2. `INSTRUMENT_QUANTITY_EXPOSURE`

두 번째 룰은 signed exposure 기준이다.

- BUY는 노출 증가
- SELL는 노출 감소
- 상태 기준은 `APPROVED`, `PARTIALLY_FILLED`, `FILLED`

## 6. 데이터 모델에서 중요한 점

- `orders(strategy_run_id, client_order_id)` unique
- `market_price(instrument_id, price_date)` unique
- `position(strategy_run_id, instrument_id)` unique
- `fill(order_id)`는 unique가 아니므로 다중 fill 허용
- `portfolio_snapshot`은 전략 실행 단위 시계열 스냅샷

## 7. 테스트 상태

- `OrderControllerIntegrationTest`
  - MARKET / LIMIT 주문
  - 리스크 거절
  - 중복 주문
  - 포트폴리오 요약
  - 실현손익
  - 시장데이터 상태
- `RealizedPnlCalculatorTest`
  - 평균단가 기반 realized PnL 계산

## 8. AI가 리뷰 시 먼저 볼 것

1. 주문 상태 전이 불변성
2. 리스크 승인과 노출 계산의 동시성
3. 포지션이 0 이하가 될 때 평균단가 정책
4. 대시보드 SQL의 정확성과 인덱스 적합성
5. 시장데이터 실패 시 재시도/백오프 전략 부재

## 9. 다음 우선순위

1. 동시성 안전한 주문 승인/노출 계산
2. 현금/계좌 기반 리스크
3. 부분체결, 취소, 만료 같은 고급 상태
4. 숏 포지션 및 고급 PnL 회계
5. 인증/권한과 운영 관측성
