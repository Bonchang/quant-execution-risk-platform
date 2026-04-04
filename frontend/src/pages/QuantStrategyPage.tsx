import { useQuery } from '@tanstack/react-query';
import { Link, useParams } from 'react-router-dom';
import {
  Line,
  LineChart,
  ResponsiveContainer,
  Tooltip,
  XAxis,
  YAxis,
} from 'recharts';
import { Badge } from '../components/Badge';
import { SectionCard } from '../components/SectionCard';
import { SignalStrengthBar } from '../components/SignalStrengthBar';
import { apiClient } from '../lib/api/client';
import { formatDateTime, formatMoney, metricValue } from '../lib/format';

export function QuantStrategyPage() {
  const { runId = '' } = useParams();
  const strategyQuery = useQuery({
    queryKey: ['quant-strategy', runId],
    queryFn: () => apiClient.getQuantStrategy(runId),
    enabled: Boolean(runId),
  });

  if (strategyQuery.isLoading) {
    return <div className="page-grid"><section className="app-panel">전략 상세를 불러오는 중입니다.</section></div>;
  }

  if (strategyQuery.isError || !strategyQuery.data) {
    return <div className="page-grid"><section className="app-panel">전략 상세를 불러오지 못했습니다.</section></div>;
  }

  const latestSignal = strategyQuery.data.signalRows[strategyQuery.data.signalRows.length - 1] ?? {};
  const signalStrength = Math.round(Number(latestSignal.position ?? 0) * 100);

  return (
    <div className="app-stack">
      <section className="hero-card hero-card--quant">
        <div className="hero-card__eyebrow">
          <span>{strategyQuery.data.instrumentSymbol}</span>
          <Badge label="퀀트 전략" tone="live" />
        </div>
        <strong className="hero-card__value">{strategyQuery.data.strategyName}</strong>
        <p className="hero-card__meta">{formatDateTime(strategyQuery.data.generatedAt)}</p>
        <SignalStrengthBar value={Number.isFinite(signalStrength) ? signalStrength : 0} />
        <div className="mini-metrics">
          {Object.entries(strategyQuery.data.metrics).map(([key, value]) => (
            <div key={key}>
              <span>{key}</span>
              <strong>{metricValue(value)}</strong>
            </div>
          ))}
        </div>
      </section>

      <div className="detail-grid">
        <SectionCard title="전략 성과" subtitle="equity curve 기준">
          <div className="chart-panel">
            <ResponsiveContainer width="100%" height="100%">
              <LineChart data={strategyQuery.data.equityCurveRows}>
                <XAxis dataKey="price_date" tickFormatter={(value) => String(value).slice(5)} />
                <YAxis hide />
                <Tooltip />
                <Line type="monotone" dataKey="equity_curve" stroke="#00a5a5" strokeWidth={2.5} dot={false} />
              </LineChart>
            </ResponsiveContainer>
          </div>
        </SectionCard>

        <SectionCard title="연결 종목" subtitle="실제 시세와 전략을 브리지합니다.">
          {strategyQuery.data.linkedInstrument ? (
            <div className="metric-list">
              <div>
                <span>종목</span>
                <strong>{strategyQuery.data.linkedInstrument.symbol}</strong>
              </div>
              <div>
                <span>현재가</span>
                <strong>{formatMoney(strategyQuery.data.linkedInstrument.lastPrice, 2)}</strong>
              </div>
              <div>
                <span>상태</span>
                <strong>{strategyQuery.data.linkedInstrument.stale ? 'STALE' : 'LIVE'}</strong>
              </div>
            </div>
          ) : (
            <p className="muted">연결된 종목 정보를 찾지 못했습니다.</p>
          )}
          <div className="panel-actions">
            <Link className="primary-button" to={`/stocks/${strategyQuery.data.instrumentSymbol}`}>종목 상세로 이동</Link>
          </div>
        </SectionCard>
      </div>

      <SectionCard title="전략 설정" subtitle="복잡한 백테스트 설정도 사용자에게는 읽기 쉬운 키/값 형태로 보여줍니다.">
        <div className="mini-metrics">
          {Object.entries(strategyQuery.data.config).map(([key, value]) => (
            <div key={key}>
              <span>{key}</span>
              <strong>{metricValue(value)}</strong>
            </div>
          ))}
        </div>
      </SectionCard>

      <div className="detail-grid">
        <SectionCard title="최근 시그널" subtitle="signals.csv에서 가져온 최신 흐름">
          <div className="activity-list">
            {strategyQuery.data.signalRows.slice(-6).reverse().map((row, index) => (
              <div className="activity-row" key={`${String(row.price_date)}-${index}`}>
                <div>
                  <strong>{String(row.price_date)}</strong>
                  <p>raw {metricValue(row.raw_signal)} / target {metricValue(row.target_exposure)}</p>
                </div>
                <div>
                  <span>position {metricValue(row.position)}</span>
                  <p>vol {metricValue(row.rolling_vol)}</p>
                </div>
              </div>
            ))}
          </div>
        </SectionCard>

        <SectionCard title="최근 주문 흐름" subtitle="전략이 운영 주문과 어떻게 이어졌는지 보여줍니다.">
          <div className="activity-list">
            {strategyQuery.data.recentOrders.map((order) => (
              <div className="activity-row" key={`${order.orderId}-${order.updatedAt}`}>
                <div>
                  <strong>Order #{order.orderId}</strong>
                  <p>{order.side} / {order.quantity.toFixed(2)}주</p>
                </div>
                <div>
                  <Badge label={order.status} tone={order.status === 'FILLED' ? 'success' : order.status === 'WORKING' ? 'warning' : 'neutral'} />
                  <p>{formatDateTime(order.updatedAt)}</p>
                </div>
              </div>
            ))}
          </div>
        </SectionCard>
      </div>
    </div>
  );
}
