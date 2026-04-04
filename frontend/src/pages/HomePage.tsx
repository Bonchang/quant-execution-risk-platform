import { useQuery } from '@tanstack/react-query';
import { Link } from 'react-router-dom';
import { AssetSummaryCard } from '../components/AssetSummaryCard';
import { Badge } from '../components/Badge';
import { InsightCard } from '../components/InsightCard';
import { ModeToggle } from '../components/ModeToggle';
import { SignalStrengthBar } from '../components/SignalStrengthBar';
import { apiClient } from '../lib/api/client';
import { formatDateTime, formatMoney, formatPercent, metricValue } from '../lib/format';
import { useMode } from '../lib/mode/ModeContext';

const toneMap = {
  positive: 'success',
  warning: 'warning',
  accent: 'live',
  neutral: 'neutral',
} as const;

export function HomePage() {
  const { mode } = useMode();
  const homeQuery = useQuery({
    queryKey: ['app-home'],
    queryFn: () => apiClient.getAppHome(),
    refetchInterval: 5000,
  });

  if (homeQuery.isLoading) {
    return <div className="page-grid"><section className="app-panel">앱 홈을 불러오는 중입니다.</section></div>;
  }

  if (homeQuery.isError || !homeQuery.data) {
    return <div className="page-grid"><section className="app-panel">홈 데이터를 불러오지 못했습니다.</section></div>;
  }

  const { assetSummary, marketConnection, highlights, featuredStocks, quantSpotlight } = homeQuery.data;

  return (
    <div className="app-stack">
      <section className="home-hero">
        <div className="home-hero__intro">
          <span className="eyebrow">QERP Investing App</span>
          <h1>일반 사용자도 퀀트 시그널을 읽고 바로 매매할 수 있는 투자앱</h1>
          <p>
            실시간 시세, 주문 전 점검, 전략 기반 인사이트를 하나의 흐름으로 묶었습니다.
            {mode === 'quant' ? ' 지금은 퀀트 모드라 전략 근거와 리스크 문구를 더 깊게 보여줍니다.' : ' 일반 투자 모드라 핵심 숫자와 행동 버튼을 먼저 보여줍니다.'}
          </p>
        </div>
        <ModeToggle />
      </section>

      <div className="home-grid">
        <AssetSummaryCard
          totalAssets={assetSummary.totalAssets}
          cashAmount={assetSummary.cashAmount}
          investedAmount={assetSummary.investedAmount}
          totalPnl={assetSummary.totalPnl}
          returnRate={assetSummary.returnRate}
          snapshotAt={assetSummary.snapshotAt}
        />
        <section className="app-panel">
          <div className="panel-header">
            <div>
              <h2>시장 연결 상태</h2>
              <p>실시간 quote 기준 운영 상태를 먼저 읽습니다.</p>
            </div>
            <Badge label={marketConnection.status} tone={marketConnection.stale ? 'stale' : 'live'} />
          </div>
          <div className="metric-list">
            <div>
              <span>마지막 quote</span>
              <strong>{formatDateTime(marketConnection.lastQuoteReceivedAt)}</strong>
            </div>
            <div>
              <span>stale quote</span>
              <strong>{marketConnection.staleQuoteCount}건</strong>
            </div>
            <div>
              <span>source</span>
              <strong>{marketConnection.source}</strong>
            </div>
          </div>
          <div className="panel-actions">
            <Link className="primary-button" to="/discover">종목 탐색</Link>
            <Link className="ghost-button" to="/portfolio">내 자산 보기</Link>
          </div>
        </section>
      </div>

      <section className="section-block">
        <div className="panel-header">
          <div>
            <h2>오늘의 핵심 포인트</h2>
            <p>복잡한 도메인 용어 대신, 일반 사용자가 바로 읽을 수 있는 문장으로 바꿨습니다.</p>
          </div>
        </div>
        <div className="card-grid">
          {highlights.map((highlight) => (
            <InsightCard
              key={highlight.title}
              title={highlight.title}
              body={highlight.body}
              tone={toneMap[highlight.tone as keyof typeof toneMap] ?? 'neutral'}
            />
          ))}
        </div>
      </section>

      {quantSpotlight ? (
        <section className="section-block">
          <div className="panel-header">
            <div>
              <h2>오늘의 퀀트 인사이트</h2>
              <p>{quantSpotlight.strategyName}</p>
            </div>
            <Link className="ghost-button" to={`/quant/strategies/${quantSpotlight.runId}`}>전략 상세</Link>
          </div>
          <div className="insight-spotlight">
            <div>
              <strong>{quantSpotlight.instrumentSymbol}</strong>
              <p>{quantSpotlight.signalHeadline}</p>
              <span className="muted">{formatDateTime(quantSpotlight.generatedAt)}</span>
            </div>
            <div>
              <SignalStrengthBar value={quantSpotlight.signalStrength} />
              <div className="mini-metrics">
                {Object.entries(quantSpotlight.metrics).slice(0, 3).map(([key, value]) => (
                  <div key={key}>
                    <span>{key}</span>
                    <strong>{metricValue(value)}</strong>
                  </div>
                ))}
              </div>
            </div>
          </div>
        </section>
      ) : null}

      <section className="section-block">
        <div className="panel-header">
          <div>
            <h2>추천 종목</h2>
            <p>시세 변화가 살아 있는 종목을 카드형으로 먼저 보여줍니다.</p>
          </div>
        </div>
        <div className="stock-card-grid">
          {featuredStocks.map((stock) => {
            const positive = stock.changePercent >= 0;
            return (
              <Link className="stock-card" key={stock.symbol} to={`/stocks/${stock.symbol}`}>
                <div className="stock-card__header">
                  <div>
                    <strong>{stock.symbol}</strong>
                    <p>{stock.name}</p>
                  </div>
                  <Badge label={stock.stale ? 'STALE' : 'LIVE'} tone={stock.stale ? 'stale' : 'live'} />
                </div>
                <strong className="stock-card__price">{formatMoney(stock.lastPrice, 2)}</strong>
                <span className={positive ? 'text-up' : 'text-down'}>
                  {positive ? '+' : ''}
                  {formatPercent(stock.changePercent)}
                </span>
                <p className="muted">{stock.reason}</p>
              </Link>
            );
          })}
        </div>
      </section>
    </div>
  );
}
