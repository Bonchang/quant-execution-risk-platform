# Quant Execution Risk Platform

시장 데이터를 기반으로 전략 주문을 생성하고, 사전 리스크 검증과 체결 시뮬레이션을 거쳐 포트폴리오 성과를 분석하는 플랫폼.

## MVP Scope

첫 버전에서는 아래 기능만 구현한다.

- CSV 기반 시장 데이터 적재
- 전략 1개 실행
- 전략 신호를 주문으로 변환
- 주문 제출 API
- 사전 리스크 검증
- 단순 체결 시뮬레이션
- 포지션 및 손익 계산
- 기본 성과 지표 분석

## Out of Scope

첫 버전에서는 아래 기능은 제외한다.

- 웹 프론트엔드
- 실시간 스트리밍
- 멀티 유저 인증
- 부분 체결
- 고급 리스크 모델
- 머신러닝 전략
- Kafka / Redis / WebSocket

## Project Structure

```text
quant-execution-risk-platform/
  java-service/
  python-research/
  docs/
