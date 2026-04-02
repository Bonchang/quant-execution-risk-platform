# QERP 프로젝트 종합 분석 문서 (AI 핸드오버용)

> 대상 리포지토리: `quant-execution-risk-platform`  
> 작성 목적: 다른 AI/엔지니어가 이 프로젝트의 목적, 현재 구현 범위, 기술적 제약, 향후 확장 포인트를 빠르게 파악하고 실질적인 조언을 제공할 수 있도록 돕는 기준 문서

---

## 1) 한 줄 요약

QERP(Quant Execution Risk Platform)는 **전략 주문을 생성하고, 룰 기반 리스크를 평가한 뒤, 승인 주문을 즉시 체결 처리하여 포지션까지 반영**하는 백엔드 중심 MVP를 Java/Spring Boot로 구현한 프로젝트다.

---

## 2) 프로젝트의 본질적 목적

이 프로젝트는 단순 주문 API가 아니라, 아래 운영 흐름을 **감사가능(auditable)하게 연결**하는 데 목적이 있다.

1. 전략 실행 컨텍스트(`StrategyRun`) 하에서 주문 생성
2. 주문 단위 리스크 룰 평가(다중 룰)
3. 승인된 주문만 실행 처리
4. 체결(`fill`) 및 포지션(`position`) 업데이트
5. 대시보드를 통해 현재 상태 관측

즉, “정량 전략 주문의 **통제된 실행 파이프라인**”을 최소 단위로 완성하려는 플랫폼이다.

---

## 3) 현재 구현 상태: 어디까지 되었는가

### 3.1 이미 구현된 것 (실행 가능)

- Spring Boot 애플리케이션 + PostgreSQL + Flyway 기반 실행환경
- 핵심 도메인 엔티티/테이블
  - `instrument`, `market_price`, `strategy_run`, `orders`, `risk_check_result`, `fill`, `position`
- 주문 생성 API
  - `POST /orders`
- 리스크 엔진 추상화 + 룰 2종
  - `MAX_ORDER_QUANTITY`
  - `INSTRUMENT_QUANTITY_EXPOSURE`
- 주문 상태 전이
  - `CREATED -> APPROVED/REJECTED -> FILLED(승인 시)`
- 최소 실행 모델
  - 승인 주문 즉시 체결
  - 최신 시장가 또는 기본가격으로 체결가 결정
- 포지션 반영
  - BUY 시 가중평균단가 반영
  - SELL 시 수량 차감
- 운영 대시보드 (정적 프론트 + 백엔드 집계 API)
  - `/` (UI), `/dashboard/overview`, `/dashboard/options`, `/dashboard/seed-demo`
- 외부 시세 수집 기능(옵션)
  - `POST /market-data/ingest`
  - `GET /market-data/status`
  - 스케줄러 연동 가능(설정 기반)

### 3.2 아직 미구현/부분 구현

- `PortfolioSnapshot` 도메인
- 현금/계좌 기반 고급 리스크
- 부분체결/슬리피지/수수료 등 고급 실행정책
- 실거래 브로커 연동
- 인증/권한
- 실시간 이벤트 스트리밍/메시지 브로커

---

## 4) 아키텍처 분석

## 4.1 레이어 구조

- **API Layer**: `OrderController`, `DashboardController`, `MarketDataController`
- **Application Layer**: `OrderService`, `RiskEvaluationService`, `OrderExecutionService`, `DashboardService`, `MarketDataIngestionService`
- **Persistence Layer**: JPA Entity + Spring Data Repository
- **Infra Layer**: PostgreSQL, Flyway, Spring Scheduler, RestTemplate

## 4.2 핵심 플로우 (주문)

1. `POST /orders` 요청 수신
2. `strategyRunId`, `instrumentId` 존재성 검증
3. `orders`에 `CREATED` 저장
4. 모든 `RiskRule` 실행 + `risk_check_result` 룰별 저장
5. 전체 통과 시 `APPROVED`, 아니면 `REJECTED`
6. `APPROVED`면 실행 서비스가 즉시 체결 처리
7. `fill` 저장 및 `position` upsert
8. 주문 상태 `FILLED`로 갱신

### 특이점

- 리스크 결과를 룰별로 항상 저장하므로 사후 분석에 유리
- 리스크 실패 주문은 fill/position side effect가 발생하지 않음
- 상태 전이/실행/포지션 업데이트가 단일 서비스 계층에서 순차 수행되는 MVP형 구조

---

## 5) 데이터 모델 요약 (실제 코드 기준)

## 5.1 주요 테이블

- `instrument`: 종목 마스터 (symbol 유니크)
- `strategy_run`: 전략 실행 컨텍스트
- `market_price`: 종목별 시계열 가격
- `orders`: 주문 (strategy_run_id + client_order_id 유니크)
- `risk_check_result`: 주문별 룰 평가 로그
- `fill`: 주문 체결 결과 (order_id 유니크)
- `position`: 전략런-종목 단위 포지션 (strategy_run_id + instrument_id 유니크)

## 5.2 무결성/중복 방지

- 주문 멱등성에 가까운 제약: `(strategy_run_id, client_order_id)` unique
- 단일 주문 다중 체결 미지원: `fill(order_id)` unique
- 포지션 단일 레코드 유지: `(strategy_run_id, instrument_id)` unique
- `market_price`는 V4에서 `(instrument_id, price_date)` unique 강제

---

## 6) API 계약/동작 분석

## 6.1 주문

### `POST /orders`

입력:
- `strategyRunId`, `instrumentId`, `side(BUY|SELL)`, `quantity`, `orderType(MARKET|LIMIT)`, `clientOrderId`

동작:
- 유효성 검증 실패: 400
- 존재하지 않는 strategy/instrument: 400
- 중복 clientOrderId(동일 strategyRun): 409
- 정상 생성: 201 + 최종 상태(`REJECTED` 또는 `FILLED`) 반환

## 6.2 대시보드

- `GET /dashboard/overview?limit=20`
  - 상태 집계, 최근 주문/리스크/체결/포지션 반환
- `GET /dashboard/options`
  - 주문 입력용 strategy run, instrument 목록
- `POST /dashboard/seed-demo`
  - 데모 데이터 생성

## 6.3 시세 수집

- `GET /market-data/status`
  - 활성화 여부, API Key 설정 여부, 마지막 실행 결과
- `POST /market-data/ingest`
  - 즉시 수집 실행
  - API Key 미설정 시 실패 메시지 포함 결과 반환(HTTP 202)

---

## 7) 리스크 엔진 분석

## 7.1 설계

- `RiskRule` 인터페이스를 구현한 컴포넌트를 리스트로 주입받아 순차 실행
- 각 룰 실행결과를 `RiskRuleResult(ruleName, passed, message)`로 표준화
- 결과를 `risk_check_result` 테이블에 영속화

## 7.2 구현된 룰

1. **MAX_ORDER_QUANTITY**
   - 단일 주문 수량 상한 비교
2. **INSTRUMENT_QUANTITY_EXPOSURE**
   - 동일 종목의 `APPROVED` + `FILLED` 누적 수량 + 신규 주문 수량이 한도를 넘는지 체크

## 7.3 확장성

- 신규 룰은 `RiskRule`만 구현하면 쉽게 추가 가능
- 단, 룰 실행 순서 보장이 필요한 경우 `@Order` 등 명시 정책이 필요할 수 있음

---

## 8) 실행/체결/포지션 로직 분석

## 8.1 실행 모델

- 현재는 “승인 즉시 체결” 단일 모델
- 체결가 산정:
  1) `market_price` 최신 `close_price`
  2) 없으면 `execution.default-fill-price`

## 8.2 포지션 업데이트

- BUY
  - `newQty = existingQty + fillQty`
  - `newAvg = (기존원가 + 신규원가) / newQty`
- SELL
  - `newQty = existingQty - fillQty`
  - `newQty <= 0`이면 평균단가 0으로 초기화

## 8.3 현재 모델의 한계

- 부분체결 없음(주문당 fill 1개)
- Short 포지션(음수 수량) 시 평균단가 처리 정책이 단순함
- 슬리피지/수수료/체결 지연/호가 기반 매칭 미지원

---

## 9) 프론트엔드 대시보드 분석

- 정적 자산(`index.html`, `app.js`, `app.css`) 기반의 운영 관찰 UI
- 기능
  - 데모 데이터 생성
  - 시세 수집 수동 실행
  - 주문 입력/전송
  - 상태 카드/테이블 시각화
  - 5초 자동 새로고침
- 목적
  - 백엔드 플로우가 “눈으로 검증” 가능하도록 하는 운영 보조 도구

즉, 제품 UI라기보다는 **MVP 검증 콘솔**에 가깝다.

---

## 10) 테스트/품질 상태 분석

- `OrderControllerIntegrationTest` 1개 클래스 중심 통합 테스트
- Testcontainers(PostgreSQL) 기반으로 실제 DB 흐름 검증
- 커버되는 주요 시나리오
  - 주문 성공 + fill/position 생성 확인
  - 중복 client order id 충돌
  - 리스크 초과로 REJECTED + no fill
  - strategyRun 누락 시 400
  - market-data ingest/status 기본 동작

### 품질 관점 평가

- 장점: “주문→리스크→체결→포지션” 핵심 경로를 실DB로 테스트
- 보완점:
  - 룰별 단위 테스트 부족
  - 포지션 계산 엣지케이스(SELL 연속, 음수포지션 등) 강화 필요
  - Dashboard SQL 질의 정확성/성능 테스트 부재

---

## 11) 설정/실행 관점 분석

## 11.1 주요 설정

- DB: `spring.datasource.*`
- JPA: `ddl-auto: validate`
- 리스크 한도:
  - `risk.max-order-quantity`
  - `risk.max-instrument-quantity`
- 실행 기본 체결가: `execution.default-fill-price`
- 시세 수집:
  - `market-data.enabled`
  - `market-data.api-key`
  - `market-data.poll-ms`
  - `market-data.max-instruments-per-run`

## 11.2 운영 의미

- 스키마는 Flyway가 단독 관리하므로 이식성이 높음
- 기본 설정에서 시세수집은 비활성화되어 안전하게 시작 가능
- API Key 미설정 상태에서도 시스템은 graceful degradation으로 동작

---

## 12) 기술부채 및 리스크 포인트 (중요)

1. **트랜잭션/동시성**
   - 노출 한도 룰이 합산 조회 후 저장하는 방식이라 동시 주문에서 경쟁조건 가능
2. **주문 멱등 처리 강화 필요**
   - 현재 DB unique 기반 충돌 처리만 제공
3. **실행 모델 단순화**
   - 실거래 현실성과 괴리 존재
4. **포지션 도메인 정책 미정의**
   - Short, 청산 후 재진입, 실현손익 등이 체계화되지 않음
5. **관측성 부족**
   - 메트릭/트레이싱/알림 체계 부재
6. **보안 미적용**
   - 인증/권한 없음
7. **시장데이터 상태 저장소 인메모리**
   - `MarketDataStatusService`는 재시작 시 상태 유실

---

## 13) 다른 AI가 이 프로젝트를 도울 때의 실전 가이드

## 13.1 AI가 먼저 이해해야 할 핵심 규칙

- 주문 결과는 “생성 직후 최종 상태”로 귀결될 수 있다 (`REJECTED`/`FILLED`).
- 리스크 결과는 룰별로 무조건 저장된다.
- fill은 주문당 1건 가정이다.
- 포지션은 전략런+종목별 단일 레코드 upsert다.

## 13.2 AI에게 권장되는 우선 개선 제안 순서

1. **동시성 안전한 리스크 승인 설계**
   - DB 락/원자적 업데이트/낙관적 락 검토
2. **실행 도메인 고도화**
   - 부분체결 모델 + 주문상태 세분화
3. **포지션/성과 도메인 확장**
   - `PortfolioSnapshot`, 실현/미실현 PnL
4. **운영 관측성**
   - 메트릭(Prometheus), 로그 상관관계, 알림
5. **보안 및 멀티유저 모델**
   - 인증/권한/테넌시

## 13.3 AI가 코드리뷰 시 집중해야 할 체크리스트

- 상태 전이 불변성 위반 가능성
- DB unique/foreign key와 JPA 매핑 일치 여부
- decimal scale/rounding 일관성
- 대시보드 SQL의 성능(정렬/인덱스)
- 외부 API 실패/지연 시 재시도/백오프 부재 여부

---

## 14) 추천 로드맵 (MVP 이후)

### Phase 1 (안정화)

- 리스크 평가 + 주문 승인 트랜잭션 원자성 강화
- 포지션 계산 단위/통합 테스트 확대
- 에러 코드/도메인 예외 체계 정규화

### Phase 2 (도메인 고도화)

- 다중 fill, 부분체결, 취소/만료 상태 도입
- PortfolioSnapshot + PnL 계산 체계
- 실행정책(슬리피지/수수료) 모듈화

### Phase 3 (운영화)

- 인증/권한, 감사로그 표준화
- 메시지 브로커 기반 비동기 이벤트 파이프라인
- 메트릭/알림/추적 기반 SRE 운영체계

---

## 15) 결론

이 리포는 “정량 주문의 통제된 실행”이라는 핵심 문제를 **작지만 끊김 없는 수직 슬라이스**로 구현한 상태다.  
즉, 단순 CRUD가 아니라 주문-리스크-체결-포지션이 실제로 이어지는 MVP이며, 향후에는 동시성/실행현실성/운영관측성/보안이 주요 확장축이 된다.

다른 AI가 조언할 때는 기능 추가 자체보다, 먼저 **도메인 불변성(상태전이/리스크 승인 원자성/포지션 일관성)**을 강화하는 방향으로 권고하는 것이 가장 효과적이다.
