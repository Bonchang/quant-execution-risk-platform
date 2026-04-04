import { Badge } from './Badge';

function money(value: number | null | undefined) {
  return Number(value ?? 0).toLocaleString('ko-KR', { minimumFractionDigits: 2, maximumFractionDigits: 2 });
}

function percent(value: number | null | undefined) {
  return `${Number(value ?? 0).toFixed(2)}%`;
}

export function QuoteHero({
  symbol,
  name,
  market,
  lastPrice,
  changePercent,
  bidPrice,
  askPrice,
  stale,
  marketStatus,
}: {
  symbol: string;
  name: string;
  market: string;
  lastPrice: number;
  changePercent: number;
  bidPrice: number;
  askPrice: number;
  stale: boolean;
  marketStatus: string;
}) {
  const positive = changePercent >= 0;

  return (
    <section className="quote-hero">
      <div className="quote-hero__header">
        <div>
          <p className="quote-hero__symbol">{symbol}</p>
          <h1>{name}</h1>
          <span className="muted">{market}</span>
        </div>
        <Badge label={marketStatus} tone={stale ? 'stale' : 'live'} />
      </div>
      <div className="quote-hero__body">
        <strong>{money(lastPrice)}</strong>
        <span className={positive ? 'text-up' : 'text-down'}>
          {positive ? '+' : ''}
          {percent(changePercent)}
        </span>
      </div>
      <div className="quote-hero__footer">
        <div>
          <span>매수 예상</span>
          <strong>{money(askPrice)}</strong>
        </div>
        <div>
          <span>매도 예상</span>
          <strong>{money(bidPrice)}</strong>
        </div>
      </div>
    </section>
  );
}
