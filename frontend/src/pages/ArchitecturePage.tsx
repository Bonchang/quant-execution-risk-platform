import { SectionCard } from '../components/SectionCard';

export function ArchitecturePage() {
  return (
    <div className="page-grid">
      <section className="hero-panel">
        <p>Architecture</p>
        <h1>Quote-driven execution architecture</h1>
        <p>
          실시간 quote, 주문 리스크, 체결, 현금·포지션 정합성, outbox 후속처리를 도메인 관점에서 정리한
          설명 화면입니다.
        </p>
      </section>

      <div className="two-col">
        <SectionCard title="Domain Flow">
          <ul className="info-list">
            <li><strong>Account / CashBalance</strong><span>주문 예약과 fill settlement의 기준 계좌</span></li>
            <li><strong>Order / Fill</strong><span>주문 상태와 실제 체결 가격·수량 기록</span></li>
            <li><strong>Position / PortfolioSnapshot</strong><span>전략별 보유와 mark-to-market 평가</span></li>
            <li><strong>Outbox</strong><span>후속 처리와 감사 가능한 이벤트 레이어</span></li>
          </ul>
        </SectionCard>
        <SectionCard title="Execution Flow">
          <ul className="info-list">
            <li><strong>Market BUY</strong><span>latest ask + slippage</span></li>
            <li><strong>Market SELL</strong><span>latest bid + slippage</span></li>
            <li><strong>Limit Order</strong><span>bid/ask 기준 executable 여부 판단 후 WORKING 또는 FILLED</span></li>
            <li><strong>Quote Refresh</strong><span>WORKING 주문 재평가와 stale warning 기록</span></li>
          </ul>
        </SectionCard>
      </div>

      <div className="two-col">
        <SectionCard title="Consistency Rules">
          <ul className="info-list">
            <li><strong>Cash</strong><span>BUY는 예약 후 정산, SELL은 proceeds 즉시 반영</span></li>
            <li><strong>Position</strong><span>SELL은 보유 롱 범위 내에서만 허용</span></li>
            <li><strong>Snapshot</strong><span>fill/outbox/quote 이벤트 이후 mark-to-market 재계산</span></li>
          </ul>
        </SectionCard>
        <SectionCard title="Research Integration">
          <ul className="info-list">
            <li><strong>Artifacts</strong><span>report.json, equity_curve.csv, trades.csv, signals.csv</span></li>
            <li><strong>Runtime Bridge</strong><span>운영 콘솔에서 최신 research run과 주문/포지션을 함께 해석</span></li>
          </ul>
        </SectionCard>
      </div>
    </div>
  );
}
