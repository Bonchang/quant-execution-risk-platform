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
import { OrderTicket } from '../components/OrderTicket';
import { QuoteHero } from '../components/QuoteHero';
import { SectionCard } from '../components/SectionCard';
import { SignalStrengthBar } from '../components/SignalStrengthBar';
import { apiClient } from '../lib/api/client';
import { useAuth } from '../lib/auth/AuthContext';
import { formatDateTime, formatMoney, metricValue } from '../lib/format';
import { useMode } from '../lib/mode/ModeContext';

export function StockDetailPage() {
  const { symbol = '' } = useParams();
  const { mode } = useMode();
  const { token, logout } = useAuth();
  const stockQuery = useQuery({
    queryKey: ['stock', symbol, token],
    queryFn: () => apiClient.getStock(symbol, token, logout),
    enabled: Boolean(symbol),
    refetchInterval: 5000,
  });

  if (stockQuery.isLoading) {
    return <div className="page-grid"><section className="app-panel">종목 상세를 불러오는 중입니다.</section></div>;
  }

  if (stockQuery.isError || !stockQuery.data) {
    return <div className="page-grid"><section className="app-panel">종목을 찾지 못했습니다.</section></div>;
  }

  const { stock, priceSeries, quantInsight, riskSummary, tradeContext, recentOrders, recentExecutions } = stockQuery.data;

  return (
    <div className="app-stack">
      <QuoteHero
        symbol={stock.symbol}
        name={stock.name}
        market={stock.market}
        lastPrice={stock.lastPrice}
        changePercent={stock.changePercent}
        bidPrice={stock.bidPrice}
        askPrice={stock.askPrice}
        stale={stock.stale}
        marketStatus={stock.marketStatus}
      />

      <div className="detail-grid">
        <SectionCard title="가격 추이" subtitle="최근 종가 기반 흐름">
          <div className="chart-panel">
            <ResponsiveContainer width="100%" height="100%">
              <LineChart data={priceSeries}>
                <XAxis dataKey="date" tickFormatter={(value) => String(value).slice(5)} />
                <YAxis domain={['dataMin - 1', 'dataMax + 1']} hide />
                <Tooltip />
                <Line type="monotone" dataKey="closePrice" stroke="#2c7be5" strokeWidth={2.5} dot={false} />
              </LineChart>
            </ResponsiveContainer>
          </div>
        </SectionCard>

        <OrderTicket
          symbol={stock.symbol}
          bidPrice={stock.bidPrice}
          askPrice={stock.askPrice}
          tradeContext={tradeContext}
        />
      </div>

      <div className="detail-grid">
        <SectionCard title="왜 이 종목인가" subtitle={quantInsight.strategyName}>
          <div className="metric-list">
            <div>
              <span>현재 시그널</span>
              <strong>{quantInsight.headline}</strong>
            </div>
            <div>
              <span>추세 판단</span>
              <strong>{quantInsight.trendLabel}</strong>
            </div>
            <div>
              <span>변동성</span>
              <strong>{quantInsight.volatilityLabel}</strong>
            </div>
          </div>
          <p className="muted">{quantInsight.summary}</p>
          <SignalStrengthBar value={quantInsight.signalStrength} />
          <ul className="bullet-list">
            {quantInsight.reasons.map((reason) => (
              <li key={reason}>{reason}</li>
            ))}
          </ul>
          {mode === 'quant' ? (
            <div className="mini-metrics">
              {Object.entries(quantInsight.metrics).map(([key, value]) => (
                <div key={key}>
                  <span>{key}</span>
                  <strong>{metricValue(value)}</strong>
                </div>
              ))}
            </div>
          ) : null}
        </SectionCard>

        <SectionCard title="주문 전 점검" subtitle="실시간 시세 기준으로 주문 위험을 먼저 보여줍니다.">
          <div className="metric-list">
            <div>
              <span>주문 가능 현금</span>
              <strong>{formatMoney(riskSummary.availableCash, 2)}</strong>
            </div>
            <div>
              <span>최대 매수 수량</span>
              <strong>{riskSummary.maxAffordableQuantity.toFixed(4)}주</strong>
            </div>
            <div>
              <span>예상 매수 단가</span>
              <strong>{formatMoney(riskSummary.estimatedBuyPrice, 2)}</strong>
            </div>
          </div>
          <p className="muted">{riskSummary.executionHint}</p>
          <div className="inline-actions">
            <Badge label={riskSummary.staleQuote ? 'STALE QUOTE' : 'LIVE QUOTE'} tone={riskSummary.staleQuote ? 'stale' : 'live'} />
            <span className="muted">{riskSummary.staleMessage}</span>
          </div>
        </SectionCard>
      </div>

      <div className="detail-grid">
        <SectionCard title="최근 주문" subtitle="이 종목에서 발생한 주문 흐름">
          <div className="activity-list">
            {recentOrders.map((item, index) => (
              <div className="activity-row" key={`${item.type}-${item.occurredAt}-${index}`}>
                <div>
                  <strong>{item.title}</strong>
                  <p>{formatDateTime(item.occurredAt)}</p>
                </div>
                <div>
                  <Badge label={item.status} tone={item.status === 'FILLED' ? 'success' : item.status === 'WORKING' ? 'warning' : 'neutral'} />
                  <p>{item.quantity.toFixed(2)} / {item.price ? formatMoney(item.price, 2) : '-'}</p>
                </div>
              </div>
            ))}
          </div>
        </SectionCard>

        <SectionCard title="체결 흐름" subtitle="실제 체결된 내역을 따로 보여줍니다.">
          <div className="activity-list">
            {recentExecutions.map((item, index) => (
              <div className="activity-row" key={`${item.type}-${item.occurredAt}-${index}`}>
                <div>
                  <strong>{item.title}</strong>
                  <p>{formatDateTime(item.occurredAt)}</p>
                </div>
                <div>
                  <span>{item.quantity.toFixed(2)}주</span>
                  <p>{item.price ? formatMoney(item.price, 2) : '-'}</p>
                </div>
              </div>
            ))}
          </div>
          <div className="panel-actions">
            <Link className="ghost-button" to="/orders">내 주문 보기</Link>
            <Link className="ghost-button" to="/quant">퀀트 전략 보기</Link>
          </div>
        </SectionCard>
      </div>
    </div>
  );
}
