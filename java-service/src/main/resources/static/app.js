const state = {
  timerId: null,
  token: localStorage.getItem('qerp.jwt') || '',
};

const summaryCards = document.getElementById('summary-cards');
const statusCards = document.getElementById('status-cards');
const marketDataStatus = document.getElementById('marketdata-status');
const marketDataResult = document.getElementById('marketdata-result');
const quotesTable = document.getElementById('quotes-table');
const accountsTable = document.getElementById('accounts-table');
const ordersTable = document.getElementById('orders-table');
const riskTable = document.getElementById('risk-table');
const fillsTable = document.getElementById('fills-table');
const positionsTable = document.getElementById('positions-table');
const portfolioKpis = document.getElementById('portfolio-kpis');
const portfolioTable = document.getElementById('portfolio-table');
const portfolioLatest = document.getElementById('portfolio-latest');
const portfolioTrend = document.getElementById('portfolio-trend');
const portfolioTrendCaption = document.getElementById('portfolio-trend-caption');
const outboxTable = document.getElementById('outbox-table');
const eventFeed = document.getElementById('event-feed');
const orderResult = document.getElementById('order-result');
const tokenResult = document.getElementById('token-result');
const researchSummary = document.getElementById('research-summary');
const orderForm = document.getElementById('order-form');
const loginForm = document.getElementById('login-form');
const accountSelect = document.getElementById('account-select');
const strategyRunSelect = document.getElementById('strategy-run-select');
const instrumentSelect = document.getElementById('instrument-select');
const refreshBtn = document.getElementById('refresh-btn');
const seedBtn = document.getElementById('seed-btn');
const ingestBtn = document.getElementById('ingest-btn');
const snapshotBtn = document.getElementById('snapshot-btn');
const autoRefreshToggle = document.getElementById('auto-refresh');

function fmt(value) {
  return value === null || value === undefined || value === '' ? '-' : String(value);
}

function fmtTime(value) {
  return value ? value.replace('T', ' ') : '-';
}

function fmtPercent(value) {
  return `${Number(value || 0).toFixed(2)}%`;
}

function fmtMoney(value) {
  return Number(value || 0).toLocaleString('ko-KR', { minimumFractionDigits: 2, maximumFractionDigits: 2 });
}

function fmtSigned(value) {
  const numeric = Number(value || 0);
  const sign = numeric > 0 ? '+' : '';
  return `${sign}${fmtMoney(numeric)}`;
}

function signClass(value) {
  const numeric = Number(value || 0);
  if (numeric > 0) return 'pos';
  if (numeric < 0) return 'neg';
  return '';
}

function badge(label, className = '') {
  return `<span class="badge ${className}">${label}</span>`;
}

function renderTable(tableEl, headers, rows) {
  const head = `<thead><tr>${headers.map((h) => `<th>${h}</th>`).join('')}</tr></thead>`;
  const body = rows.length
    ? rows.map((row) => `<tr>${row.map((c) => `<td>${c}</td>`).join('')}</tr>`).join('')
    : `<tr><td colspan="${headers.length}">데이터 없음</td></tr>`;
  tableEl.innerHTML = `${head}<tbody>${body}</tbody>`;
}

function authHeaders(extra = {}) {
  const headers = { ...extra };
  if (state.token) {
    headers.Authorization = `Bearer ${state.token}`;
  }
  return headers;
}

async function apiFetch(path, options = {}) {
  const response = await fetch(path, {
    ...options,
    headers: authHeaders(options.headers || {}),
  });
  const text = await response.text();
  let body = {};
  try {
    body = text ? JSON.parse(text) : {};
  } catch {
    body = { raw: text };
  }
  if (!response.ok) {
    throw new Error(`${path} -> ${response.status}: ${body.message || body.error || text}`);
  }
  return body;
}

function renderOverview(data) {
  const summary = data.summary || {};
  const portfolio = data.portfolioSummary || {};
  const research = data.researchSummary || {};
  const quoteSummary = data.quoteSummary || {};
  const marketHealth = data.marketDataHealth || {};

  const cards = [
    ['총 주문', fmt(summary.totalOrders)],
    ['체결 주문', fmt(summary.filledOrders)],
    ['거절 주문', fmt(summary.rejectedOrders)],
    ['체결률', fmtPercent(summary.fillRatePercent)],
    ['거절률', fmtPercent(summary.rejectionRatePercent)],
    ['실시간 시세 수', fmt(quoteSummary.totalQuotes)],
    ['Stale 시세', fmt(quoteSummary.staleQuotes)],
    ['총 평가금액', fmtMoney(portfolio.totalMarketValue)],
    ['총 손익', fmtSigned(portfolio.totalPnl), signClass(portfolio.totalPnl)],
  ];
  summaryCards.innerHTML = cards.map(([label, value, cls]) => `<div class="kpi"><div class="label">${label}</div><div class="value ${cls || ''}">${value}</div></div>`).join('');

  const portfolioCards = [
    ['계좌', portfolio.accountCode || '-'],
    ['전략', portfolio.strategyName || '-'],
    ['스냅샷 시각', fmtTime(portfolio.snapshotAt)],
    ['미실현 손익', fmtSigned(portfolio.unrealizedPnl), signClass(portfolio.unrealizedPnl)],
    ['실현 손익', fmtSigned(portfolio.realizedPnl), signClass(portfolio.realizedPnl)],
  ];
  portfolioKpis.innerHTML = portfolioCards.map(([label, value, cls]) => `<div class="kpi"><div class="label">${label}</div><div class="value ${cls || ''}">${value}</div></div>`).join('');

  portfolioLatest.innerHTML = `
    <div class="latest-item"><span>계좌</span><strong>${fmt(portfolio.accountCode)}</strong></div>
    <div class="latest-item"><span>전략</span><strong>${fmt(portfolio.strategyName)}</strong></div>
    <div class="latest-item"><span>총 평가금액</span><strong>${fmtMoney(portfolio.totalMarketValue)}</strong></div>
    <div class="latest-item"><span>총 손익</span><strong class="${signClass(portfolio.totalPnl)}">${fmtSigned(portfolio.totalPnl)}</strong></div>
    <div class="latest-item"><span>수익률</span><strong class="${signClass(portfolio.returnRate)}">${fmtPercent(portfolio.returnRate)}</strong></div>
  `;

  researchSummary.textContent = JSON.stringify(research, null, 2);

  const statuses = Object.keys(data.statusCounts || {}).sort();
  statusCards.innerHTML = statuses.length
    ? statuses.map((status) => `<div class="status"><div class="name">${status}</div><div class="count">${data.statusCounts[status]}</div></div>`).join('')
    : '<div class="status"><div class="name">ORDERS</div><div class="count">0</div></div>';

  eventFeed.innerHTML = [
    data.recentOrders?.[0] ? `최근 주문 #${data.recentOrders[0].id} -> ${data.recentOrders[0].status}` : null,
    data.recentOutboxEvents?.[0] ? `최근 outbox ${data.recentOutboxEvents[0].eventType}` : null,
    marketHealth.lastRunStatus ? `시장데이터 상태 ${marketHealth.lastRunStatus}` : null,
    research.runId ? `최신 연구 ${research.runId}` : null,
  ].filter(Boolean).map((line) => `<li>${line}</li>`).join('') || '<li>아직 이벤트 없음</li>';

  renderTable(accountsTable, ['Account', 'Owner', 'Currency', 'AvailableCash', 'ReservedCash', 'UpdatedAt'],
    (data.accountSummaries || []).map((a) => [fmt(a.accountCode), fmt(a.ownerName), fmt(a.baseCurrency), fmtMoney(a.availableCash), fmtMoney(a.reservedCash), fmtTime(a.updatedAt)]));

  renderTable(ordersTable, ['ID', 'Account', 'ClientOrderId', 'Status', 'Side', 'ReqQty', 'LimitPx', 'ReservedCash', 'FilledQty', 'RemainQty', 'Type', 'TIF', 'Instrument', 'Strategy', 'ExpiresAt', 'UpdatedAt'],
    (data.recentOrders || []).map((o) => [fmt(o.id), fmt(o.accountCode), fmt(o.clientOrderId), fmt(o.status), fmt(o.side), fmt(o.requestedQuantity), fmt(o.limitPrice), fmtMoney(o.reservedCashAmount), fmt(o.filledQuantity), fmt(o.remainingQuantity), fmt(o.orderType), fmt(o.timeInForce), fmt(o.instrumentSymbol), fmt(o.strategyName), fmtTime(o.expiresAt), fmtTime(o.updatedAt)]));

  renderTable(riskTable, ['ID', 'OrderID', 'Rule', 'Result', 'Message', 'CheckedAt'],
    (data.recentRiskChecks || []).map((r) => [fmt(r.id), fmt(r.orderId), fmt(r.ruleName), `<span class="badge ${r.passed ? 'pass' : 'fail'}">${r.passed ? 'PASS' : 'FAIL'}</span>`, fmt(r.message), fmtTime(r.checkedAt)]));

  renderTable(fillsTable, ['ID', 'OrderID', 'Instrument', 'Qty', 'Price', 'FilledAt'],
    (data.recentFills || []).map((f) => [fmt(f.id), fmt(f.orderId), fmt(f.instrumentSymbol), fmt(f.fillQuantity), fmt(f.fillPrice), fmtTime(f.filledAt)]));

  renderTable(positionsTable, ['ID', 'Strategy', 'Instrument', 'NetQty', 'AvgPrice', 'UpdatedAt'],
    (data.positions || []).map((p) => [fmt(p.id), fmt(p.strategyName), fmt(p.instrumentSymbol), fmt(p.netQuantity), fmt(p.averagePrice), fmtTime(p.updatedAt)]));

  renderTable(portfolioTable, ['ID', 'Account', 'StrategyRun', 'Strategy', 'SnapshotAt', 'MarketValue', 'UnrealizedPnL', 'RealizedPnL', 'TotalPnL', 'ReturnRate'],
    (data.recentPortfolioSnapshots || []).map((p) => [fmt(p.id), fmt(p.accountCode), fmt(p.strategyRunId), fmt(p.strategyName), fmtTime(p.snapshotAt), fmtMoney(p.totalMarketValue), fmtSigned(p.unrealizedPnl), fmtSigned(p.realizedPnl), fmtSigned(p.totalPnl), fmtPercent(p.returnRate)]));

  renderTable(quotesTable, ['Symbol', 'Market', 'Last', 'Bid', 'Ask', 'Change%', 'Source', 'QuoteTime', 'ReceivedAt', 'Freshness'],
    (data.recentQuotes || []).map((q) => [fmt(q.symbol), fmt(q.market), fmtMoney(q.lastPrice), fmtMoney(q.bidPrice), fmtMoney(q.askPrice), fmtPercent(q.changePercent), fmt(q.source), fmtTime(q.quoteTime), fmtTime(q.receivedAt), q.stale ? badge('STALE', 'fail') : badge('LIVE', 'pass')]));

  renderTable(outboxTable, ['ID', 'Aggregate', 'AggregateId', 'Event', 'Status', 'CreatedAt', 'ProcessedAt'],
    (data.recentOutboxEvents || []).map((e) => [fmt(e.id), fmt(e.aggregateType), fmt(e.aggregateId), fmt(e.eventType), fmt(e.processingStatus), fmtTime(e.createdAt), fmtTime(e.processedAt)]));

  renderPortfolioTrend(data.recentPortfolioSnapshots || []);
}

function renderPortfolioTrend(snapshots) {
  if (!snapshots?.length) {
    portfolioTrend.innerHTML = '';
    portfolioTrendCaption.textContent = '스냅샷 이력이 있으면 추이를 표시합니다.';
    return;
  }
  const ordered = [...snapshots].reverse();
  const values = ordered.map((s) => Number(s.totalPnl || 0));
  const min = Math.min(...values);
  const max = Math.max(...values);
  const range = Math.max(max - min, 1);
  const width = 640;
  const height = 180;
  const padding = { top: 16, right: 16, bottom: 26, left: 54 };
  const innerWidth = width - padding.left - padding.right;
  const innerHeight = height - padding.top - padding.bottom;
  const toX = (idx) => padding.left + (idx / Math.max(values.length - 1, 1)) * innerWidth;
  const toY = (value) => padding.top + (1 - ((value - min) / range)) * innerHeight;
  const points = values.map((value, idx) => `${toX(idx).toFixed(2)},${toY(value).toFixed(2)}`).join(' ');
  portfolioTrend.innerHTML = `<polyline fill="none" stroke="${values[values.length - 1] >= 0 ? '#2f855a' : '#c53030'}" stroke-width="3" points="${points}" />`;
  portfolioTrendCaption.textContent = `최근 ${ordered.length}개 스냅샷 기준 총손익 추이 · 최신 ${fmtSigned(values[values.length - 1])}`;
}

function renderMarketDataStatus(status) {
  const result = status.lastResult || {};
  marketDataStatus.innerHTML = [
    ['활성화', status.enabled ? 'ON' : 'OFF'],
    ['API Key', status.apiKeyConfigured ? 'SET' : 'EMPTY'],
    ['소스', fmt(status.source)],
    ['마지막 실행', fmtTime(status.lastRunAt)],
    ['마지막 시세 수신', fmtTime(status.lastQuoteReceivedAt)],
    ['Stale 여부', status.stale ? 'YES' : 'NO'],
    ['Stale 수', fmt(status.staleQuoteCount)],
    ['대상 종목수', fmt(result.totalInstruments)],
    ['성공', fmt(result.successCount)],
    ['실패', fmt(result.failureCount)],
  ].map(([name, value]) => `<div class="mstat"><div class="name">${name}</div><div class="count">${value}</div></div>`).join('');
  marketDataResult.textContent = JSON.stringify(result, null, 2);
}

function fillOptions(options) {
  accountSelect.innerHTML = '';
  (options.accounts || []).forEach((account) => {
    const option = document.createElement('option');
    option.value = String(account.id);
    option.textContent = `${account.accountCode} (${account.ownerName})`;
    accountSelect.appendChild(option);
  });

  strategyRunSelect.innerHTML = '';
  (options.strategyRuns || []).forEach((strategyRun) => {
    const option = document.createElement('option');
    option.value = String(strategyRun.id);
    option.dataset.accountId = String(strategyRun.accountId);
    option.textContent = `${strategyRun.id} - ${strategyRun.strategyName}`;
    strategyRunSelect.appendChild(option);
  });

  instrumentSelect.innerHTML = '';
  (options.instruments || []).forEach((instrument) => {
    const option = document.createElement('option');
    option.value = String(instrument.id);
    option.textContent = `${instrument.symbol} (${fmt(instrument.latestClosePrice)})`;
    instrumentSelect.appendChild(option);
  });

  syncStrategyRunByAccount();
}

function syncStrategyRunByAccount() {
  const accountId = accountSelect.value;
  for (const option of strategyRunSelect.options) {
    option.hidden = accountId && option.dataset.accountId !== accountId;
  }
  const visible = Array.from(strategyRunSelect.options).find((option) => !option.hidden);
  if (visible) {
    strategyRunSelect.value = visible.value;
  }
}

async function loadOptions() {
  fillOptions(await apiFetch('/dashboard/options'));
}

async function loadOverview() {
  renderOverview(await apiFetch('/dashboard/overview?limit=20'));
}

async function loadMarketDataStatus() {
  renderMarketDataStatus(await apiFetch('/market-data/status'));
}

async function login(evt) {
  evt.preventDefault();
  const formData = new FormData(loginForm);
  const body = await apiFetch('/auth/token', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({
      username: String(formData.get('username')),
      password: String(formData.get('password')),
    }),
  });
  state.token = body.accessToken;
  localStorage.setItem('qerp.jwt', state.token);
  tokenResult.textContent = JSON.stringify(body, null, 2);
  await Promise.all([loadOptions(), loadOverview(), loadMarketDataStatus()]);
  setAutoRefresh(autoRefreshToggle.checked);
}

async function submitOrder(evt) {
  evt.preventDefault();
  const formData = new FormData(orderForm);
  const body = await apiFetch('/orders', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({
      accountId: Number(formData.get('accountId')),
      strategyRunId: Number(formData.get('strategyRunId')),
      instrumentId: Number(formData.get('instrumentId')),
      side: String(formData.get('side')),
      quantity: String(formData.get('quantity')),
      orderType: String(formData.get('orderType')),
      limitPrice: formData.get('limitPrice') ? String(formData.get('limitPrice')) : null,
      timeInForce: String(formData.get('timeInForce')),
      clientOrderId: String(formData.get('clientOrderId')),
    }),
  });
  orderResult.textContent = JSON.stringify(body, null, 2);
  await Promise.all([loadOverview(), loadMarketDataStatus()]);
}

async function createDemoData() {
  orderResult.textContent = JSON.stringify(await apiFetch('/dashboard/seed-demo', { method: 'POST' }), null, 2);
  await Promise.all([loadOptions(), loadOverview(), loadMarketDataStatus()]);
}

async function ingestMarketData() {
  marketDataResult.textContent = JSON.stringify(await apiFetch('/market-data/ingest', { method: 'POST' }), null, 2);
  await Promise.all([loadOverview(), loadMarketDataStatus()]);
}

async function refreshPortfolioSnapshots() {
  orderResult.textContent = JSON.stringify(await apiFetch('/dashboard/portfolio-snapshots/refresh', { method: 'POST' }), null, 2);
  await Promise.all([loadOverview(), loadMarketDataStatus()]);
}

function setAutoRefresh(enabled) {
  if (state.timerId) {
    clearInterval(state.timerId);
    state.timerId = null;
  }
  if (enabled && state.token) {
    state.timerId = setInterval(() => {
      Promise.all([loadOverview(), loadMarketDataStatus()]).catch((err) => {
        orderResult.textContent = `refresh error: ${err.message}`;
      });
    }, 5000);
  }
}

loginForm.addEventListener('submit', (evt) => login(evt).catch((err) => { tokenResult.textContent = err.message; }));
orderForm.addEventListener('submit', (evt) => submitOrder(evt).catch((err) => { orderResult.textContent = err.message; }));
refreshBtn.addEventListener('click', () => Promise.all([loadOptions(), loadOverview(), loadMarketDataStatus()]).catch((err) => { orderResult.textContent = err.message; }));
seedBtn.addEventListener('click', () => createDemoData().catch((err) => { orderResult.textContent = err.message; }));
ingestBtn.addEventListener('click', () => ingestMarketData().catch((err) => { marketDataResult.textContent = err.message; }));
snapshotBtn.addEventListener('click', () => refreshPortfolioSnapshots().catch((err) => { orderResult.textContent = err.message; }));
autoRefreshToggle.addEventListener('change', (evt) => setAutoRefresh(evt.target.checked));
accountSelect.addEventListener('change', syncStrategyRunByAccount);

tokenResult.textContent = state.token ? '저장된 JWT 사용 중' : '먼저 JWT를 발급하세요.';
if (state.token) {
  Promise.all([loadOptions(), loadOverview(), loadMarketDataStatus()])
    .then(() => setAutoRefresh(true))
    .catch((err) => {
      orderResult.textContent = err.message;
    });
}
