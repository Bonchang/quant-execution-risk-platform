# MVP Scope and Status

## 1. MVP Goal

최소한의 운영 경로를 끝까지 연결한다.

- 전략 실행 컨텍스트 생성
- 주문 생성/저장
- 리스크 판정
- 승인 주문 실행
- 체결/포지션 반영
- 상태 시각화

## 2. Included (Delivered)

- [x] Spring Boot + Gradle 서비스 부트스트랩
- [x] PostgreSQL + Flyway 기반 스키마 관리
- [x] Core entities (`Instrument`, `MarketPrice`, `StrategyRun`, `Order`)
- [x] `POST /orders`
- [x] 리스크 엔진 스켈레톤 (`RiskRule`, `RiskCheckResult`)
- [x] 룰 2개 (max order qty, per-instrument exposure)
- [x] 승인 주문 최소 실행 처리
- [x] `Fill`/`Position` 영속화
- [x] 진행상황 대시보드 (`/`, `/dashboard/overview`)

## 3. Included (In Progress)

- [ ] `PortfolioSnapshot` 모델/집계

## 4. Excluded (Current)

- 실거래 브로커 어댑터
- 고급 실행 전략(부분체결, 슬리피지 모델 고도화)
- 현금/계좌 기반 리스크
- 인증/권한
- 실시간 스트리밍/메시지 브로커
- 고급 최적화/ML 전략

## 5. Exit Criteria for Current MVP Phase

1. 주문 API가 리스크 결과에 따라 `REJECTED` 또는 `FILLED`까지 전이한다.
2. 체결/포지션 데이터가 DB에서 일관되게 추적 가능하다.
3. Flyway migration으로 신규 환경에서 재현 가능한 초기화가 가능하다.
4. 대시보드에서 운영 상태를 한 화면에서 확인 가능하다.
