# Render + Supabase Free Deployment

QERP 1차 공개 배포 기준은 `Render Free Web Service + Supabase Free Postgres`다.  
목표는 기능을 더 붙이는 것이 아니라 `홈/종목/주문/포트폴리오` 핵심 흐름이 웹 URL에서 안정적으로 열리는 상태를 만드는 것이다.

## 1. 배포 구성

- App: Render `Web Service`
- Runtime: Docker
- Dockerfile: `java-service/Dockerfile`
- DB: Supabase Free Postgres
- Health check: `/actuator/health`
- 배포 모드: `MARKET_DATA_ENABLED=false`

이 모드에서는 실시간 시세 자동 수집보다 공개 데모 안정성을 우선한다.

## 2. Supabase 준비

1. Supabase에서 새 프로젝트를 만든다.
2. Render와 가까운 리전을 선택한다.
3. Database 연결 정보에서 Session Pooler 또는 IPv4 호환 호스트를 사용한다.
4. 아래 값을 확보한다.

- JDBC URL 예시: `jdbc:postgresql://<host>:5432/postgres?sslmode=require`
- Username: `postgres.<project-ref>` 형식
- Password: 프로젝트 DB 비밀번호

`sslmode=require`는 무료 배포 기준 기본값으로 고정한다.

## 3. Render 준비

1. GitHub 저장소를 Render에 연결한다.
2. `Blueprint` 또는 새 `Web Service` 생성 시 루트의 `render.yaml`을 사용한다.
3. 서비스 타입은 `Free`로 유지한다.
4. 아래 환경변수를 입력한다.

- `SPRING_DATASOURCE_URL`
- `DB_USERNAME`
- `DB_PASSWORD`
- `JWT_SECRET`
- `RESEARCH_ARTIFACTS_PATH=/tmp/artifacts`
- `MARKET_DATA_ENABLED=false`

선택값:

- `FINNHUB_API_KEY`
- `MARKET_DATA_POLL_MS`

1차 공개 배포에서는 선택값 없이 시작하는 편이 안전하다.

## 4. 첫 배포 후 확인

먼저 `/actuator/health`가 `UP`인지 확인한다. 이후 아래 순서로 스모크 테스트를 한다.

- `GET /`
- `GET /discover`
- `GET /stocks/DEMO_AAPL`
- `GET /app/home`
- `POST /auth/token`
- 인증 후 `GET /app/portfolio`
- 인증 후 `GET /app/orders`

관리자 토큰으로 `POST /dashboard/seed-demo`를 호출한 뒤 다시 아래를 확인한다.

- `GET /app/home`
- `GET /app/stocks/DEMO_AAPL`
- `GET /app/portfolio`
- `GET /market-data/quotes`

## 5. 장애 확인 포인트

- Render 로그에서 Spring Boot 시작 실패, Flyway 실패, DB SSL 오류를 먼저 본다.
- 애플리케이션 500은 `X-Correlation-Id`와 함께 로그를 추적한다.
- Supabase 연결 실패가 나면 host, username, password, `sslmode=require`를 다시 확인한다.

## 6. 무료 플랜 운영 수칙

- Render Free는 유휴 시 슬립될 수 있으므로 첫 요청이 느릴 수 있다.
- Render 파일시스템은 영속 저장소가 아니므로 업로드/artifact 저장 경로로 쓰지 않는다.
- Supabase Free는 용량이 작으므로 데모 데이터 위주로 유지한다.
- 무료 정책은 바뀔 수 있으니 실제 배포 직전 Render/Supabase 대시보드 제한을 다시 확인한다.

## 7. 다음 단계

공개 URL이 안정화되면 아래 순서로 확장한다.

1. `MARKET_DATA_ENABLED=true` + 느린 poll 주기 적용
2. Finnhub API key 연결
3. research artifact 영속 스토리지 분리
4. 배포 smoke test를 CI와 연결
