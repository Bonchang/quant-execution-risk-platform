import { useQuery } from '@tanstack/react-query';
import { Link } from 'react-router-dom';
import { Badge } from '../components/Badge';
import { SectionCard } from '../components/SectionCard';
import { SignalStrengthBar } from '../components/SignalStrengthBar';
import { apiClient } from '../lib/api/client';
import { formatDateTime, formatMoney, formatPercent, metricValue } from '../lib/format';

export function QuantPage() {
  const quantQuery = useQuery({
    queryKey: ['quant-overview'],
    queryFn: () => apiClient.getQuantOverview(),
  });

  if (quantQuery.isLoading) {
    return <div className="page-grid"><section className="app-panel">퀀트 인사이트를 불러오는 중입니다.</section></div>;
  }

  if (quantQuery.isError || !quantQuery.data) {
    return <div className="page-grid"><section className="app-panel">퀀트 인사이트를 불러오지 못했습니다.</section></div>;
  }

  return (
    <div className="app-stack">
      {quantQuery.data.featuredInsight ? (
        <section className="hero-card hero-card--quant">
          <div className="hero-card__eyebrow">
            <span>대표 전략</span>
            <Badge label={quantQuery.data.featuredInsight.instrumentSymbol} tone="live" />
          </div>
          <strong className="hero-card__value">{quantQuery.data.featuredInsight.strategyName}</strong>
          <p className="hero-card__meta">{quantQuery.data.featuredInsight.headline}</p>
          <SignalStrengthBar value={quantQuery.data.featuredInsight.signalStrength} />
          <div className="mini-metrics">
            {Object.entries(quantQuery.data.featuredInsight.metrics).map(([key, value]) => (
              <div key={key}>
                <span>{key}</span>
                <strong>{metricValue(value)}</strong>
              </div>
            ))}
          </div>
        </section>
      ) : null}

      <SectionCard title="전략 카드" subtitle="일반 사용자도 시그널 기반 주문으로 진입할 수 있도록 설명형 카드로 구성했습니다.">
        <div className="stock-card-grid">
          {quantQuery.data.strategies.map((strategy) => (
            <Link className="stock-card" key={strategy.runId} to={`/quant/strategies/${strategy.runId}`}>
              <div className="stock-card__header">
                <div>
                  <strong>{strategy.instrumentSymbol}</strong>
                  <p>{strategy.strategyName}</p>
                </div>
                <Badge label="전략" tone="live" />
              </div>
              <div className="stock-card__body">
                <strong className="stock-card__price">{strategy.lastPrice ? formatMoney(strategy.lastPrice, 2) : '-'}</strong>
                <span className={(strategy.changePercent ?? 0) >= 0 ? 'text-up' : 'text-down'}>
                  {strategy.changePercent != null ? formatPercent(strategy.changePercent) : '-'}
                </span>
              </div>
              <div className="stock-card__footer">
                <span>{formatDateTime(strategy.generatedAt)}</span>
                <span>Sharpe {metricValue(strategy.metrics.sharpe)}</span>
              </div>
            </Link>
          ))}
        </div>
      </SectionCard>
    </div>
  );
}
