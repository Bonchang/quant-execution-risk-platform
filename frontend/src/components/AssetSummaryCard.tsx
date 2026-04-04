import { Badge } from './Badge';

function formatMoney(value: number | null | undefined) {
  return Number(value ?? 0).toLocaleString('ko-KR', { minimumFractionDigits: 0, maximumFractionDigits: 0 });
}

function formatPercent(value: number | null | undefined) {
  return `${(Number(value ?? 0) * 100).toFixed(2)}%`;
}

export function AssetSummaryCard({
  totalAssets,
  cashAmount,
  investedAmount,
  totalPnl,
  returnRate,
  snapshotAt,
}: {
  totalAssets: number;
  cashAmount: number;
  investedAmount: number;
  totalPnl: number;
  returnRate: number;
  snapshotAt: string | null;
}) {
  const positive = totalPnl >= 0;

  return (
    <section className="hero-card">
      <div className="hero-card__eyebrow">
        <span>내 자산</span>
        <Badge label={positive ? '수익 구간' : '변동 구간'} tone={positive ? 'success' : 'warning'} />
      </div>
      <strong className="hero-card__value">{formatMoney(totalAssets)}원</strong>
      <p className="hero-card__meta">
        예수금 {formatMoney(cashAmount)}원 / 투자금 {formatMoney(investedAmount)}원
      </p>
      <div className="hero-card__stats">
        <div>
          <span>총 손익</span>
          <strong className={positive ? 'text-up' : 'text-down'}>
            {positive ? '+' : ''}
            {formatMoney(totalPnl)}원
          </strong>
        </div>
        <div>
          <span>수익률</span>
          <strong className={positive ? 'text-up' : 'text-down'}>{formatPercent(returnRate)}</strong>
        </div>
        <div>
          <span>기준 시각</span>
          <strong>{snapshotAt ? snapshotAt.replace('T', ' ') : '-'}</strong>
        </div>
      </div>
    </section>
  );
}
