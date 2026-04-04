# Quant Execution Risk Platform (QERP)

QERP는 정량 전략 시그널이 실제 주문으로 연결될 때 필요한 금융 백엔드 흐름과 퀀트 리서치 흐름을 함께 보여주기 위한 포트폴리오 프로젝트다.  
핵심은 `전략 실행 -> 주문 생성 -> 리스크 판정 -> 체결/포지션/현금 반영 -> 포트폴리오 스냅샷 -> 리서치 아티팩트 조회`를 하나의 데모로 묶는 것이다.

## 1. 문제 정의

현업형 정량 시스템은 단순 백테스트만으로 설명되지 않는다. 아래가 동시에 필요하다.

1. 주문과 포지션이 계좌 단위로 통제되는 백엔드
2. 현금/포지션/노출 기준 리스크 차단
3. 체결 결과와 포트폴리오 성과의 영속화
4. 연구 결과와 운영 결과를 함께 보여주는 인터페이스

QERP는 이 4가지를 로컬에서 재현 가능한 수준으로 묶은 준운영형 포트폴리오다.

## 2. 아키텍처

### Java Service

- Spring Boot 3.5
- PostgreSQL + Flyway
- 주문/리스크/체결/포지션/현금/스냅샷 도메인
- JWT + 역할 기반 접근 제어
- Outbox 기반 후속 처리
- Micrometer + Prometheus + Actuator

### Python Research

- `pandas`, `numpy`, `sqlalchemy`, `pyyaml`, `plotly`
- PostgreSQL `market_price` 직접 조회
- `volatility-targeted moving average crossover` 전략
- 리포트/시그널/트레이드/equity curve artifact 생성

### 주요 운영 흐름

1. `POST /strategy-runs`
2. `POST /orders`
3. 리스크 평가
   - 최대 주문 수량
   - 계좌별 종목 노출
   - 보유 롱 포지션 범위 내 SELL
   - BUY 예상 현금 범위
4. 체결 또는 `WORKING`
5. outbox 이벤트 적재 및 스냅샷 후속 처리
6. `/dashboard/overview`, `/research/runs`로 운영/리서치 결과 확인

## 3. 실행 방법

### 3.1 로컬 인프라

```bash
docker compose -f compose.yml up -d
```

실행 후:

- Java Service: [http://localhost:8080](http://localhost:8080)
- Prometheus: [http://localhost:9090](http://localhost:9090)

### 3.2 Java 단독 실행

```bash
cd java-service
./gradlew bootRun
```

### 3.3 Python 리서치 실행

```bash
cd python-research
python -m pip install -e .
python -m qerp_research.run_backtest --config configs/demo_strategy.yaml --artifacts-dir artifacts
```

### 3.4 기본 JWT 계정

- `admin / admin123!`
- `trader / trader123!`
- `viewer / viewer123!`

정적 UI(`/`)에서 먼저 토큰을 발급한 뒤 API를 호출하면 된다.

## 4. 데모 시나리오

### 시나리오 A. 운영 주문과 리스크 감사

1. `admin`으로 JWT 발급
2. `데모 데이터 생성`
3. `BUY MARKET` 주문 생성
4. 대시보드에서 주문 상태, 현금 예약/정산, fill, position, snapshot, outbox 이벤트 확인
5. `BUY LIMIT`를 시장가보다 낮게 넣어 `WORKING` 상태 확인
6. `POST /orders/{id}/cancel` 또는 `/orders/expire-working`으로 후속 상태 확인

### 시나리오 B. 연구 결과와 운영 흐름 연결

1. Python 백테스트 실행
2. `python-research/artifacts/<run_id>/report.json` 생성
3. Java `/research/runs`, 대시보드 리서치 요약에서 최근 결과 확인
4. 리서치 대상 종목으로 전략 실행과 주문 흐름 설명

## 5. 기술 결정

- JWT + 역할
  - `ADMIN`, `TRADER`, `VIEWER`
  - 조회와 주문 권한을 분리
- `WORKING`, `CANCELED`, `EXPIRED`
  - LIMIT 주문의 운영 상태를 더 현실적으로 표현
- Account / CashBalance / CashLedgerEntry
  - 현금 기반 리스크와 예약/정산 흐름을 추적
- Outbox
  - 주문 후속 처리와 감사 가능한 이벤트 기록을 분리
- Research Artifact 연동
  - Python과 Java 경계를 명확하게 유지

## 6. 주요 API

- `POST /auth/token`
- `GET /accounts`
- `POST /strategy-runs`
- `GET /strategy-runs`
- `GET /strategy-runs/{id}`
- `POST /orders`
- `GET /orders`
- `GET /orders/{id}`
- `POST /orders/{id}/cancel`
- `POST /orders/expire-working`
- `GET /dashboard/overview`
- `GET /dashboard/options`
- `POST /dashboard/seed-demo`
- `POST /dashboard/portfolio-snapshots/refresh`
- `GET /research/runs`
- `GET /research/runs/{runId}`
- `GET /market-data/status`
- `POST /market-data/ingest`

## 7. 이 프로젝트로 증명한 역량

- 금융 백엔드
  - 주문 상태 머신, 계좌/현금 정산, DB 마이그레이션, 보안
- 데이터 모델링
  - 주문/체결/포지션/현금/스냅샷/outbox 모델 분리
- 리스크 통제
  - 현금, 노출, 보유 수량, 주문 수량 기준 차단
- 테스트/운영성
  - Gradle 테스트, Python 테스트, GitHub Actions, Prometheus, correlation id
- 퀀트 리서치
  - 전략 구현, 백테스트, 성과지표, artifact 생성 및 운영 연동

## 8. 문서

- [System Architecture](docs/system-architecture.md)
- [MVP Scope and Status](docs/mvp.md)
- [ERD Draft](docs/erd-draft.md)
- [AI Handover Analysis](docs/ai-handover-analysis.md)
- [ADR 001 - Outbox](docs/adr/001-outbox.md)
- [ADR 002 - No Short Selling](docs/adr/002-no-short-selling.md)
- [ADR 003 - Research Artifacts](docs/adr/003-research-artifacts.md)
