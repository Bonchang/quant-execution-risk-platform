# ADR 003: Python Research 결과는 파일 Artifact로 연동한다

## 결정

Python 리서치 결과는 DB write-back 대신 `python-research/artifacts/<run_id>` 디렉터리에 파일로 생성하고, Java 서비스는 이를 읽어 조회 API를 제공한다.

## 이유

- Python과 Java의 책임 경계를 명확하게 유지할 수 있다.
- 리서치 산출물을 재현 가능한 보고서 형태로 보존할 수 있다.
- 로컬 데모와 면접 시연에서 결과물이 눈에 보이는 형태로 남는다.
- 스키마 결합도를 낮춰 리서치 실험 속도를 유지할 수 있다.

## 결과

- `report.json`, `equity_curve.csv`, `trades.csv`, `signals.csv`가 표준 산출물이다.
- Java는 `/research/runs`, `/research/runs/{runId}`로 최신 결과를 노출한다.
- 장기적으로 필요하면 DB 적재나 object storage 업로드 단계로 확장 가능하다.
