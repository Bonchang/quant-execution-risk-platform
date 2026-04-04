# MVP Scope and Status

## 1. MVP Goal

최소한의 운영 경로를 끝까지 연결한다.

- 전략 실행 컨텍스트 준비
- 주문 생성/저장
- 리스크 판정
- 승인 주문 실행
- 체결/포지션 반영
- 포트폴리오 스냅샷 생성
- 운영 상태 시각화

## 2. Included (Delivered)

- [x] Spring Boot + Gradle 서비스 부트스트랩
- [x] PostgreSQL + Flyway 기반 스키마 관리
- [x] Core entities
  - `Instrument`
  - `MarketPrice`
  - `StrategyRun`
  - `Order`
  - `RiskCheckResult`
  - `Fill`
  - `Position`
  - `PortfolioSnapshot`
- [x] `POST /orders`
- [x] 리스크 엔진 스켈레톤 (`RiskRule`, `RiskRuleResult`, `RiskCheckResult`)
- [x] 룰 2개
  - max order quantity
  - per-instrument signed exposure
- [x] MARKET / LIMIT 주문 실행 정책
- [x] 다중 fill 저장 지원
- [x] `filled_quantity`, `remaining_quantity`, `last_executed_at` 추적
- [x] `Fill` / `Position` 영속화
- [x] 포트폴리오 스냅샷 및 KPI 계산
- [x] 진행상황 대시보드
  - `/`
  - `/dashboard/overview`
  - `/dashboard/options`
  - `/dashboard/seed-demo`
  - `/dashboard/portfolio-snapshots/refresh`
- [x] 외부 시장데이터 수집/상태 API
  - `/market-data/ingest`
  - `/market-data/status`
- [x] 통합 테스트
  - 주문 생성/거절
  - LIMIT 체결
  - 포트폴리오 요약
  - 실현손익 계산
  - 시장데이터 상태

## 3. Excluded (Current)

- 실거래 브로커 어댑터
- 고급 실행 전략
  - 슬리피지
  - 수수료
  - 취소/만료
  - 호가 기반 매칭
- 현금/계좌 기반 리스크
- 인증/권한
- 실시간 스트리밍/메시지 브로커
- 숏 포지션 회계 정책 정교화
- 고급 성과 분석
  - drawdown
  - sharpe
  - attribution

## 4. Exit Criteria for Current MVP Phase

1. 주문 API가 리스크 결과에 따라 `REJECTED`, `APPROVED`, `PARTIALLY_FILLED`, `FILLED` 상태를 일관되게 전이한다.
2. 체결/포지션/포트폴리오 스냅샷이 DB에서 추적 가능하다.
3. Flyway migration으로 신규 환경 초기화가 가능하다.
4. 대시보드에서 주문, 리스크, 체결, 포지션, 포트폴리오 상태를 확인할 수 있다.
5. 시장데이터 수집 기능이 비활성 기본값에서 안전하게 동작하고, 설정 시 활성화 가능하다.

## 5. Known Limitations

- 종목 노출 계산은 signed quantity 기준이지만 동시성 충돌 방지는 아직 단순하다.
- 포트폴리오 계산은 MVP 수준이며 숏 포지션 시 회계 정책이 단순화되어 있다.
- 시장데이터 상태는 인메모리로 유지되어 재시작 시 초기화된다.
