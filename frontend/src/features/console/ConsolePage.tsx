import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { useState } from 'react';
import { Link } from 'react-router-dom';
import {
  Line,
  LineChart,
  ResponsiveContainer,
  Tooltip,
  XAxis,
  YAxis,
} from 'recharts';
import { Badge } from '../../components/Badge';
import { DataTable } from '../../components/DataTable';
import { SectionCard } from '../../components/SectionCard';
import { useAuth } from '../../lib/auth/AuthContext';
import { apiClient } from '../../lib/api/client';
import { buildCreateOrderPayload, type CreateOrderFormState } from '../../lib/orders';
import { hasRequiredRole } from '../../lib/auth/token';

const tabs = ['Overview', 'Market', 'Orders', 'Risk & Audit', 'Portfolio', 'Research Link'] as const;
type ConsoleTab = (typeof tabs)[number];

const initialForm: CreateOrderFormState = {
  accountId: '',
  strategyRunId: '',
  instrumentId: '',
  side: 'BUY',
  quantity: '10.000000',
  orderType: 'MARKET',
  timeInForce: 'DAY',
  limitPrice: '',
  clientOrderId: `web-${Date.now()}`,
};

function money(value: number | null | undefined) {
  return Number(value ?? 0).toLocaleString('ko-KR', { minimumFractionDigits: 2, maximumFractionDigits: 2 });
}

function percent(value: number | null | undefined) {
  return `${Number(value ?? 0).toFixed(2)}%`;
}

function toneForStatus(status: string) {
  if (status === 'FILLED' || status === 'LIVE' || status === 'UP') return 'success' as const;
  if (status === 'WORKING' || status === 'STALE' || status === 'DEGRADED') return 'warning' as const;
  if (status === 'REJECTED' || status === 'FAILED') return 'danger' as const;
  return 'neutral' as const;
}

export function ConsolePage() {
  const queryClient = useQueryClient();
  const { token, role, login, logout } = useAuth();
  const [activeTab, setActiveTab] = useState<ConsoleTab>('Overview');
  const [message, setMessage] = useState('JWT를 발급한 뒤 운영 콘솔을 사용할 수 있습니다.');
  const [credentials, setCredentials] = useState({ username: 'admin', password: 'admin123!' });
  const [orderForm, setOrderForm] = useState<CreateOrderFormState>(initialForm);

  const overviewQuery = useQuery({
    queryKey: ['console-overview', token],
    queryFn: () => apiClient.getOverview(token, logout),
    enabled: Boolean(token),
    refetchInterval: token ? 5000 : false,
  });
  const timelineQuery = useQuery({
    queryKey: ['console-timeline', token],
    queryFn: () => apiClient.getTimeline(token, logout),
    enabled: Boolean(token),
    refetchInterval: token ? 5000 : false,
  });
  const optionsQuery = useQuery({
    queryKey: ['dashboard-options', token],
    queryFn: () => apiClient.getOptions(token, logout),
    enabled: Boolean(token),
  });
  const quotesQuery = useQuery({
    queryKey: ['quotes', token],
    queryFn: () => apiClient.getQuotes(token, logout),
    enabled: Boolean(token),
    refetchInterval: token ? 5000 : false,
  });
  const statusQuery = useQuery({
    queryKey: ['market-status', token],
    queryFn: () => apiClient.getMarketStatus(token, logout),
    enabled: Boolean(token),
    refetchInterval: token ? 5000 : false,
  });
  const healthQuery = useQuery({
    queryKey: ['market-health', token],
    queryFn: () => apiClient.getMarketHealth(token, logout),
    enabled: Boolean(token),
    refetchInterval: token ? 5000 : false,
  });
  const selectedOrderId = String(overviewQuery.data?.recentOrders?.[0]?.id ?? '');
  const orderDetailQuery = useQuery({
    queryKey: ['order-detail', selectedOrderId, token],
    queryFn: () => apiClient.getOrder(selectedOrderId, token, logout),
    enabled: Boolean(token && selectedOrderId),
  });
  const researchQuery = useQuery({
    queryKey: ['research-runs'],
    queryFn: () => apiClient.listResearchRuns(),
  });

  const refreshAll = async () => {
    await Promise.all([
      queryClient.invalidateQueries({ queryKey: ['console-overview'] }),
      queryClient.invalidateQueries({ queryKey: ['console-timeline'] }),
      queryClient.invalidateQueries({ queryKey: ['quotes'] }),
      queryClient.invalidateQueries({ queryKey: ['market-status'] }),
      queryClient.invalidateQueries({ queryKey: ['market-health'] }),
      queryClient.invalidateQueries({ queryKey: ['order-detail'] }),
    ]);
  };

  const loginMutation = useMutation({
    mutationFn: () => apiClient.login(credentials.username, credentials.password),
    onSuccess: async (payload) => {
      login(payload);
      setMessage(`${payload.role} 세션이 활성화되었습니다.`);
      await refreshAll();
    },
    onError: (error: Error) => setMessage(error.message),
  });

  const createOrderMutation = useMutation({
    mutationFn: () => apiClient.createOrder(buildCreateOrderPayload(orderForm), token, logout),
    onSuccess: async () => {
      setMessage('주문이 접수되었습니다.');
      setOrderForm((current) => ({ ...current, clientOrderId: `web-${Date.now()}` }));
      await refreshAll();
    },
    onError: (error: Error) => setMessage(error.message),
  });

  const seedMutation = useMutation({
    mutationFn: () => apiClient.seedDemo(token, logout),
    onSuccess: async () => {
      setMessage('데모 데이터 생성을 완료했습니다.');
      await refreshAll();
    },
    onError: (error: Error) => setMessage(error.message),
  });
  const ingestMutation = useMutation({
    mutationFn: () => apiClient.ingestMarketData(token, logout),
    onSuccess: async () => {
      setMessage('시장데이터 수집을 실행했습니다.');
      await refreshAll();
    },
    onError: (error: Error) => setMessage(error.message),
  });
  const snapshotMutation = useMutation({
    mutationFn: () => apiClient.refreshSnapshots(token, logout),
    onSuccess: async () => {
      setMessage('포트폴리오 스냅샷을 갱신했습니다.');
      await refreshAll();
    },
    onError: (error: Error) => setMessage(error.message),
  });
  const cancelMutation = useMutation({
    mutationFn: (orderId: number) => apiClient.cancelOrder(orderId, token, logout),
    onSuccess: async () => {
      setMessage('주문을 취소했습니다.');
      await refreshAll();
    },
    onError: (error: Error) => setMessage(error.message),
  });

  const canTrade = hasRequiredRole(role, ['ROLE_ADMIN', 'ROLE_TRADER']);
  const isAdmin = hasRequiredRole(role, ['ROLE_ADMIN']);

  const options = optionsQuery.data;
  const overview = overviewQuery.data;
  const latestResearch = researchQuery.data?.[0];

  if (!token) {
    return (
      <div className="page-grid">
        <section className="hero-panel">
          <p>Console Access</p>
          <h1>인증된 사용자용 운영 콘솔</h1>
          <p>JWT를 발급하면 실시간 운영 상태, 주문 입력, 리스크/감사, 포트폴리오 패널을 사용할 수 있습니다.</p>
        </section>
        <SectionCard title="Inline Login" subtitle="기본 계정: admin/admin123!, trader/trader123!, viewer/viewer123!">
          <form
            className="auth-form"
            onSubmit={(event) => {
              event.preventDefault();
              loginMutation.mutate();
            }}
          >
            <input value={credentials.username} onChange={(event) => setCredentials((current) => ({ ...current, username: event.target.value }))} />
            <input type="password" value={credentials.password} onChange={(event) => setCredentials((current) => ({ ...current, password: event.target.value }))} />
            <button className="primary-button" type="submit" disabled={loginMutation.isPending}>
              토큰 발급
            </button>
          </form>
          <p className="muted">{message}</p>
        </SectionCard>
      </div>
    );
  }

  const renderOverviewTab = () => (
    <div className="page-grid">
      <div className="kpi-grid">
        <div className="kpi-card"><span>총 주문</span><strong>{overview?.summary.totalOrders ?? 0}</strong></div>
        <div className="kpi-card"><span>WORKING</span><strong>{overview?.statusCounts.WORKING ?? 0}</strong></div>
        <div className="kpi-card"><span>Stale Quote</span><strong>{overview?.quoteSummary.staleQuotes ?? 0}</strong></div>
        <div className="kpi-card"><span>Latest Snapshot</span><strong>{overview?.portfolioSummary.snapshotAt ?? '-'}</strong></div>
        <div className="kpi-card"><span>Latest Research</span><strong>{overview?.researchSummary.runId ?? '-'}</strong></div>
      </div>
      <div className="two-col">
        <SectionCard title="Current State" subtitle="실시간 운영 상태를 먼저 읽는 패널">
          <ul className="info-list">
            <li><strong>Portfolio MV</strong><span>{money(overview?.portfolioSummary.totalMarketValue)}</span></li>
            <li><strong>Total PnL</strong><span>{money(overview?.portfolioSummary.totalPnl)}</span></li>
            <li><strong>Market Health</strong><span>{overview?.marketDataHealth.status}</span></li>
          </ul>
        </SectionCard>
        <SectionCard title="Recent Timeline">
          <div className="timeline-list">
            {(timelineQuery.data?.events ?? []).slice(0, 6).map((event) => (
              <div className="timeline-item" key={`${event.category}-${event.subjectKey}-${event.occurredAt}`}>
                <div className="inline-actions">
                  <Badge label={event.category} tone="neutral" />
                  <Badge label={event.eventType} tone={toneForStatus(event.severity)} />
                </div>
                <h4>{event.title}</h4>
                <p>{event.description}</p>
              </div>
            ))}
          </div>
        </SectionCard>
      </div>
    </div>
  );

  const renderMarketTab = () => (
    <div className="page-grid">
      <div className="two-col">
        <SectionCard title="Market Data Health">
          <ul className="info-list">
            <li><strong>Status</strong><span>{healthQuery.data?.status ?? '-'}</span></li>
            <li><strong>Source</strong><span>{statusQuery.data?.source ?? '-'}</span></li>
            <li><strong>Last Run</strong><span>{statusQuery.data?.lastRunAt ?? '-'}</span></li>
            <li><strong>Last Quote</strong><span>{statusQuery.data?.lastQuoteReceivedAt ?? '-'}</span></li>
          </ul>
        </SectionCard>
        <SectionCard title="Admin Actions" subtitle="관리자 권한에서만 노출됩니다.">
          <div className="inline-actions">
            {isAdmin ? (
              <>
                <button className="primary-button" type="button" onClick={() => seedMutation.mutate()}>Seed Demo</button>
                <button className="ghost-button" type="button" onClick={() => ingestMutation.mutate()}>Ingest Quotes</button>
                <button className="ghost-button" type="button" onClick={() => snapshotMutation.mutate()}>Refresh Snapshot</button>
              </>
            ) : (
              <p className="muted">ADMIN 권한에서만 seed/ingest/snapshot 액션을 사용할 수 있습니다.</p>
            )}
          </div>
        </SectionCard>
      </div>
      <SectionCard title="Live Quotes" subtitle="5초 polling으로 갱신되는 quote 기준 패널">
        <DataTable
          rows={quotesQuery.data ?? []}
          columns={[
            { header: 'Symbol', render: (row) => row.symbol },
            { header: 'Market', render: (row) => row.market },
            { header: 'Last', render: (row) => money(row.lastPrice) },
            { header: 'Bid', render: (row) => money(row.bidPrice) },
            { header: 'Ask', render: (row) => money(row.askPrice) },
            { header: 'Change', render: (row) => percent(row.changePercent) },
            {
              header: 'Freshness',
              render: (row) => <Badge label={row.stale ? 'STALE' : 'LIVE'} tone={row.stale ? 'stale' : 'live'} />,
            },
          ]}
        />
      </SectionCard>
    </div>
  );

  const renderOrdersTab = () => (
    <div className="page-grid">
      <SectionCard title="Create Order" subtitle="TRADER 이상 권한에서 주문 생성이 가능합니다.">
        <form
          className="form-grid"
          onSubmit={(event) => {
            event.preventDefault();
            createOrderMutation.mutate();
          }}
        >
          <label>
            Account
            <select value={orderForm.accountId} onChange={(event) => setOrderForm((current) => ({ ...current, accountId: event.target.value }))}>
              <option value="">Select</option>
              {(options?.accounts ?? []).map((account) => (
                <option key={account.id} value={account.id}>{account.accountCode}</option>
              ))}
            </select>
          </label>
          <label>
            Strategy Run
            <select value={orderForm.strategyRunId} onChange={(event) => setOrderForm((current) => ({ ...current, strategyRunId: event.target.value }))}>
              <option value="">Select</option>
              {(options?.strategyRuns ?? []).filter((run) => !orderForm.accountId || String(run.accountId) === orderForm.accountId).map((run) => (
                <option key={run.id} value={run.id}>{run.strategyName}</option>
              ))}
            </select>
          </label>
          <label>
            Instrument
            <select value={orderForm.instrumentId} onChange={(event) => setOrderForm((current) => ({ ...current, instrumentId: event.target.value }))}>
              <option value="">Select</option>
              {(options?.instruments ?? []).map((instrument) => (
                <option key={instrument.id} value={instrument.id}>{instrument.symbol}</option>
              ))}
            </select>
          </label>
          <label>
            Side
            <select value={orderForm.side} onChange={(event) => setOrderForm((current) => ({ ...current, side: event.target.value as CreateOrderFormState['side'] }))}>
              <option value="BUY">BUY</option>
              <option value="SELL">SELL</option>
            </select>
          </label>
          <label>
            Quantity
            <input value={orderForm.quantity} onChange={(event) => setOrderForm((current) => ({ ...current, quantity: event.target.value }))} />
          </label>
          <label>
            Order Type
            <select value={orderForm.orderType} onChange={(event) => setOrderForm((current) => ({ ...current, orderType: event.target.value as CreateOrderFormState['orderType'] }))}>
              <option value="MARKET">MARKET</option>
              <option value="LIMIT">LIMIT</option>
            </select>
          </label>
          <label>
            Time In Force
            <select value={orderForm.timeInForce} onChange={(event) => setOrderForm((current) => ({ ...current, timeInForce: event.target.value as CreateOrderFormState['timeInForce'] }))}>
              <option value="DAY">DAY</option>
              <option value="GTC">GTC</option>
            </select>
          </label>
          <label>
            Limit Price
            <input value={orderForm.limitPrice} onChange={(event) => setOrderForm((current) => ({ ...current, limitPrice: event.target.value }))} />
          </label>
          <label>
            Client Order ID
            <input value={orderForm.clientOrderId} onChange={(event) => setOrderForm((current) => ({ ...current, clientOrderId: event.target.value }))} />
          </label>
          <button className="primary-button" type="submit" disabled={!canTrade || createOrderMutation.isPending}>
            주문 전송
          </button>
        </form>
      </SectionCard>
      <SectionCard title="Recent Orders">
        <DataTable
          rows={overview?.recentOrders ?? []}
          columns={[
            {
              header: 'Order',
              render: (row) => (
                <Link to={`/console/orders/${row.id}`}>
                  #{row.id} {row.instrumentSymbol}
                </Link>
              ),
            },
            { header: 'Status', render: (row) => <Badge label={row.status} tone={toneForStatus(row.status)} /> },
            { header: 'Type', render: (row) => `${row.orderType} / ${row.timeInForce}` },
            { header: 'Qty', render: (row) => String(row.requestedQuantity ?? row.quantity ?? '-') },
            { header: 'Filled', render: (row) => String(row.filledQuantity) },
            { header: 'Remain', render: (row) => String(row.remainingQuantity) },
            {
              header: 'Action',
              render: (row) => row.status === 'WORKING' && canTrade
                ? <button className="ghost-button" type="button" onClick={() => cancelMutation.mutate(row.id)}>Cancel</button>
                : '-',
            },
          ]}
        />
      </SectionCard>
    </div>
  );

  const renderRiskAuditTab = () => (
    <div className="page-grid">
      <div className="two-col">
        <SectionCard title="Latest Order Risk Checks">
          <DataTable
            rows={orderDetailQuery.data?.riskChecks ?? overview?.recentRiskChecks ?? []}
            columns={[
              { header: 'Rule', render: (row) => row.ruleName },
              { header: 'Result', render: (row) => <Badge label={row.passed ? 'PASS' : 'FAIL'} tone={row.passed ? 'success' : 'danger'} /> },
              { header: 'Message', render: (row) => row.message },
            ]}
          />
        </SectionCard>
        <SectionCard title="Cash Ledger">
          <DataTable
            rows={orderDetailQuery.data?.cashLedgerEntries ?? []}
            columns={[
              { header: 'Entry', render: (row) => row.entryType },
              { header: 'Amount', render: (row) => money(row.amount) },
              { header: 'Avail After', render: (row) => money(row.availableCashAfter) },
              { header: 'Reserved After', render: (row) => money(row.reservedCashAfter) },
              { header: 'Note', render: (row) => row.note },
            ]}
          />
        </SectionCard>
      </div>
      <SectionCard title="Timeline / Outbox Audit">
        <div className="timeline-list">
          {(timelineQuery.data?.events ?? []).map((event) => (
            <div className="timeline-item" key={`${event.subjectKey}-${event.occurredAt}`}>
              <div className="inline-actions">
                <Badge label={event.category} tone="neutral" />
                <Badge label={event.eventType} tone={toneForStatus(event.severity)} />
              </div>
              <h4>{event.title}</h4>
              <p>{event.description}</p>
            </div>
          ))}
        </div>
      </SectionCard>
    </div>
  );

  const renderPortfolioTab = () => (
    <div className="page-grid">
      <div className="two-col">
        <SectionCard title="Latest Mark-to-Market">
          <ul className="info-list">
            <li><strong>Account</strong><span>{overview?.portfolioSummary.accountCode ?? '-'}</span></li>
            <li><strong>Strategy</strong><span>{overview?.portfolioSummary.strategyName ?? '-'}</span></li>
            <li><strong>Market Value</strong><span>{money(overview?.portfolioSummary.totalMarketValue)}</span></li>
            <li><strong>Total PnL</strong><span>{money(overview?.portfolioSummary.totalPnl)}</span></li>
          </ul>
        </SectionCard>
        <SectionCard title="Account Cash">
          <DataTable
            rows={overview?.accountSummaries ?? []}
            columns={[
              { header: 'Account', render: (row) => row.accountCode },
              { header: 'Available', render: (row) => money(row.availableCash) },
              { header: 'Reserved', render: (row) => money(row.reservedCash) },
            ]}
          />
        </SectionCard>
      </div>
      <SectionCard title="Snapshot Trend">
        <div className="chart-panel">
          <ResponsiveContainer width="100%" height="100%">
            <LineChart data={[...(overview?.recentPortfolioSnapshots ?? [])].reverse()}>
              <XAxis dataKey="snapshotAt" hide />
              <YAxis />
              <Tooltip />
              <Line type="monotone" dataKey="totalPnl" stroke="#0f4c81" strokeWidth={3} dot={false} />
            </LineChart>
          </ResponsiveContainer>
        </div>
      </SectionCard>
      <SectionCard title="Positions">
        <DataTable
          rows={overview?.positions ?? []}
          columns={[
            { header: 'Strategy', render: (row) => row.strategyName },
            { header: 'Instrument', render: (row) => row.instrumentSymbol },
            { header: 'Net Qty', render: (row) => String(row.netQuantity) },
            { header: 'Avg Px', render: (row) => money(row.averagePrice) },
          ]}
        />
      </SectionCard>
    </div>
  );

  const renderResearchLinkTab = () => (
    <div className="page-grid">
      <div className="two-col">
        <SectionCard title="Latest Research Run">
          <ul className="info-list">
            <li><strong>Run</strong><span>{latestResearch?.runId ?? '-'}</span></li>
            <li><strong>Strategy</strong><span>{latestResearch?.strategyName ?? '-'}</span></li>
            <li><strong>Instrument</strong><span>{latestResearch?.instrumentSymbol ?? '-'}</span></li>
          </ul>
        </SectionCard>
        <SectionCard title="Operational Bridge">
          <ul className="info-list">
            <li><strong>Live Quote Count</strong><span>{overview?.quoteSummary.totalQuotes ?? 0}</span></li>
            <li><strong>Recent Order</strong><span>{overview?.recentOrders?.[0]?.clientOrderId ?? '-'}</span></li>
            <li><strong>Portfolio Strategy</strong><span>{overview?.portfolioSummary.strategyName ?? '-'}</span></li>
          </ul>
          {latestResearch ? (
            <div className="inline-actions">
              <Link className="ghost-button" to={`/research/${latestResearch.runId}`}>리서치 상세 보기</Link>
            </div>
          ) : null}
        </SectionCard>
      </div>
    </div>
  );

  const tabContent = {
    Overview: renderOverviewTab(),
    Market: renderMarketTab(),
    Orders: renderOrdersTab(),
    'Risk & Audit': renderRiskAuditTab(),
    Portfolio: renderPortfolioTab(),
    'Research Link': renderResearchLinkTab(),
  }[activeTab];

  return (
    <div className="page-grid">
      <section className="hero-panel">
        <p>Mission Console</p>
        <h1>실시간 운영감 중심 콘솔</h1>
        <p>시세, 주문 상태, 리스크, 포트폴리오를 5초 polling 기준으로 같은 화면에서 읽습니다.</p>
        <div className="inline-actions">
          {tabs.map((tab) => (
            <button
              key={tab}
              className={tab === activeTab ? 'secondary-button' : 'ghost-button'}
              type="button"
              onClick={() => setActiveTab(tab)}
            >
              {tab}
            </button>
          ))}
          <button className="ghost-button" type="button" onClick={() => refreshAll()}>수동 새로고침</button>
        </div>
      </section>

      <p className="muted">{message}</p>
      {tabContent}
    </div>
  );
}
