import { useQuery } from '@tanstack/react-query';
import {
  Area,
  AreaChart,
  ResponsiveContainer,
  Tooltip,
  XAxis,
  YAxis,
} from 'recharts';
import { AssetSummaryCard } from '../components/AssetSummaryCard';
import { HoldingRow } from '../components/HoldingRow';
import { InlineAuthPanel } from '../components/InlineAuthPanel';
import { SectionCard } from '../components/SectionCard';
import { useAuth } from '../lib/auth/AuthContext';
import { apiClient } from '../lib/api/client';
import { formatDateTime, formatMoney } from '../lib/format';

export function PortfolioPage() {
  const { token, logout } = useAuth();
  const portfolioQuery = useQuery({
    queryKey: ['portfolio', token],
    queryFn: () => apiClient.getPortfolio(token, logout),
    enabled: Boolean(token),
    refetchInterval: token ? 5000 : false,
  });

  if (!token) {
    return <InlineAuthPanel title="내 자산 보기" subtitle="게스트 세션을 시작하면 내 paper account와 자산 스냅샷이 바로 생성됩니다." />;
  }

  if (portfolioQuery.isLoading) {
    return <div className="page-grid"><section className="app-panel">포트폴리오를 불러오는 중입니다.</section></div>;
  }

  if (portfolioQuery.isError || !portfolioQuery.data) {
    return <div className="page-grid"><section className="app-panel">포트폴리오 데이터를 불러오지 못했습니다.</section></div>;
  }

  const { assetSummary, account, holdings, assetTrend, recentExecutions } = portfolioQuery.data;

  return (
    <div className="app-stack">
      <AssetSummaryCard
        totalAssets={assetSummary.totalAssets}
        cashAmount={assetSummary.cashAmount}
        investedAmount={assetSummary.investedAmount}
        totalPnl={assetSummary.totalPnl}
        returnRate={assetSummary.returnRate}
        snapshotAt={assetSummary.snapshotAt}
      />

      <div className="detail-grid">
        <SectionCard title="내 계좌" subtitle={`${account.accountCode} / ${account.ownerName}`}>
          <div className="metric-list">
            <div>
              <span>사용 가능 현금</span>
              <strong>{formatMoney(account.availableCash, 2)}</strong>
            </div>
            <div>
              <span>예약 현금</span>
              <strong>{formatMoney(account.reservedCash, 2)}</strong>
            </div>
            <div>
              <span>기준 통화</span>
              <strong>{account.baseCurrency}</strong>
            </div>
          </div>
        </SectionCard>

        <SectionCard title="자산 추이" subtitle="최근 스냅샷 기준">
          <div className="chart-panel">
            <ResponsiveContainer width="100%" height="100%">
              <AreaChart data={assetTrend}>
                <XAxis dataKey="snapshotAt" tickFormatter={(value) => String(value).slice(5, 10)} />
                <YAxis hide />
                <Tooltip />
                <Area type="monotone" dataKey="totalAssets" stroke="#2c7be5" fill="rgba(44, 123, 229, 0.18)" />
              </AreaChart>
            </ResponsiveContainer>
          </div>
        </SectionCard>
      </div>

      <SectionCard title="보유 종목" subtitle="표 대신 카드형 holding row로 밀도를 줄였습니다.">
        <div className="stack-list">
          {holdings.map((holding) => (
            <HoldingRow key={`${holding.symbol}-${holding.strategyName}`} {...holding} />
          ))}
        </div>
      </SectionCard>

      <SectionCard title="최근 체결" subtitle="주문 완료 이후 자산 변화를 바로 추적합니다.">
        <div className="activity-list">
          {recentExecutions.map((execution) => (
            <div className="activity-row" key={`${execution.orderId}-${execution.filledAt}`}>
              <div>
                <strong>{execution.symbol}</strong>
                <p>Order #{execution.orderId}</p>
              </div>
              <div>
                <span>{execution.fillQuantity.toFixed(2)}주 / {formatMoney(execution.fillPrice, 2)}</span>
                <p>{formatDateTime(execution.filledAt)}</p>
              </div>
            </div>
          ))}
        </div>
      </SectionCard>
    </div>
  );
}
