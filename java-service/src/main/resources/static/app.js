const state = {
  timerId: null,
  options: { strategyRuns: [], instruments: [] },
};

const summaryCards = document.getElementById('summary-cards');
const statusCards = document.getElementById('status-cards');
const marketDataStatus = document.getElementById('marketdata-status');
const marketDataResult = document.getElementById('marketdata-result');
const ordersTable = document.getElementById('orders-table');
const riskTable = document.getElementById('risk-table');
const fillsTable = document.getElementById('fills-table');
const positionsTable = document.getElementById('positions-table');
const portfolioKpis = document.getElementById('portfolio-kpis');
const portfolioTable = document.getElementById('portfolio-table');
const eventFeed = document.getElementById('event-feed');
const orderResult = document.getElementById('order-result');
const orderForm = document.getElementById('order-form');
const strategyRunSelect = document.getElementById('strategy-run-select');
const instrumentSelect = document.getElementById('instrument-select');
const refreshBtn = document.getElementById('refresh-btn');
const seedBtn = document.getElementById('seed-btn');
const ingestBtn = document.getElementById('ingest-btn');
const snapshotBtn = document.getElementById('snapshot-btn');
const autoRefreshToggle = document.getElementById('auto-refresh');

function fmt(value) {
  return value === null || value === undefined ? '-' : String(value);
}

function fmtTime(value) {
  return value ? value.replace('T', ' ') : '-';
}

function fmtPercent(value) {
  return `${Number(value || 0).toFixed(1)}%`;
}

function fmtSigned(value) {
  const numeric = Number(value || 0);
  const sign = numeric > 0 ? '+' : '';
  return `${sign}${numeric.toFixed(6)}`;
}

function renderTable(tableEl, headers, rows) {
  const head = `<thead><tr>${headers.map((h) => `<th>${h}</th>`).join('')}</tr></thead>`;
  const body = rows.length
    ? rows.map((row) => `<tr>${row.map((c) => `<td>${c}</td>`).join('')}</tr>`).join('')
    : `<tr><td colspan="${headers.length}">데이터 없음</td></tr>`;
  tableEl.innerHTML = `${head}<tbody>${body}</tbody>`;
}

function renderSummary(summary) {
  const cards = [
    ['총 주문', summary.totalOrders],
    ['체결 주문', summary.filledOrders],
    ['거절 주문', summary.rejectedOrders],
    ['체결률', fmtPercent(summary.fillRatePercent)],
    ['거절률', fmtPercent(summary.rejectionRatePercent)],
  ];
  summaryCards.innerHTML = cards
    .map(([label, value]) => `<div class="kpi"><div class="label">${label}</div><div class="value">${value}</div></div>`)
    .join('');
}

function renderStatusCounts(counts) {
  const statuses = Object.keys(counts || {}).sort();
  statusCards.innerHTML = statuses.length
    ? statuses.map((status) => `<div class="status"><div class="name">${status}</div><div class="count">${counts[status]}</div></div>`).join('')
    : '<div class="status"><div class="name">ORDERS</div><div class="count">0</div></div>';
}

function renderPortfolioSummary(summary) {
  const cards = [
    ['전략', summary.strategyName || '-'],
    ['스냅샷 시각', fmtTime(summary.snapshotAt)],
    ['총 평가금액', fmt(summary.totalMarketValue)],
    ['미실현 PnL', fmtSigned(summary.unrealizedPnl)],
    ['총 PnL', fmtSigned(summary.totalPnl)],
    ['수익률', fmtPercent(summary.returnRate)],
  ];
  portfolioKpis.innerHTML = cards
    .map(([label, value]) => `<div class="kpi"><div class="label">${label}</div><div class="value">${value}</div></div>`)
    .join('');
}

function renderEventFeed(data) {
  const items = [];
  if (data.recentOrders?.[0]) {
    const o = data.recentOrders[0];
    items.push(`최근 주문 #${o.id} (${o.clientOrderId}) 상태: ${o.status}`);
  }
  if (data.recentRiskChecks?.[0]) {
    const r = data.recentRiskChecks[0];
    items.push(`최근 리스크: ${r.ruleName} => ${r.passed ? 'PASS' : 'FAIL'}`);
  }
  if (data.recentFills?.[0]) {
    const f = data.recentFills[0];
    items.push(`최근 체결: ${f.instrumentSymbol} ${f.fillQuantity}@${f.fillPrice}`);
  }
  if (data.positions?.[0]) {
    const p = data.positions[0];
    items.push(`최근 포지션: ${p.strategyName}/${p.instrumentSymbol} 수량 ${p.netQuantity}`);
  }

  eventFeed.innerHTML = (items.length ? items : ['아직 이벤트 없음'])
    .map((item) => `<li>${item}</li>`)
    .join('');
}

function renderMarketDataStatus(status) {
  const result = status.lastResult || {};
  const cards = [
    ['활성화', status.enabled ? 'ON' : 'OFF'],
    ['API Key', status.apiKeyConfigured ? 'SET' : 'EMPTY'],
    ['마지막 실행', fmtTime(status.lastRunAt)],
    ['대상 종목수', fmt(result.totalInstruments)],
    ['성공', fmt(result.successCount)],
    ['실패', fmt(result.failureCount)],
  ];

  marketDataStatus.innerHTML = cards
    .map(([name, value]) => `<div class="mstat"><div class="name">${name}</div><div class="count">${value}</div></div>`)
    .join('');

  const updated = Array.isArray(result.updatedSymbols) ? result.updatedSymbols : [];
  const failures = Array.isArray(result.failures) ? result.failures : [];
  if (updated.length || failures.length) {
    marketDataResult.textContent = JSON.stringify({ updatedSymbols: updated, failures }, null, 2);
  }
}

function renderOverview(data) {
  renderSummary(data.summary || { totalOrders: 0, filledOrders: 0, rejectedOrders: 0, fillRatePercent: 0, rejectionRatePercent: 0 });
  renderPortfolioSummary(data.portfolioSummary || {});
  renderStatusCounts(data.statusCounts || {});
  renderEventFeed(data);

  renderTable(
    ordersTable,
    ['ID', 'ClientOrderId', 'Status', 'Side', 'ReqQty', 'LimitPx', 'FilledQty', 'RemainQty', 'Type', 'Instrument', 'Strategy', 'CreatedAt', 'UpdatedAt'],
    (data.recentOrders || []).map((o) => [
      fmt(o.id),
      fmt(o.clientOrderId),
      fmt(o.status),
      fmt(o.side),
      fmt(o.requestedQuantity),
      fmt(o.limitPrice),
      fmt(o.filledQuantity),
      fmt(o.remainingQuantity),
      fmt(o.orderType),
      fmt(o.instrumentSymbol),
      fmt(o.strategyName),
      fmtTime(o.createdAt),
      fmtTime(o.updatedAt),
    ]),
  );

  renderTable(
    riskTable,
    ['ID', 'OrderID', 'Rule', 'Result', 'Message', 'CheckedAt'],
    (data.recentRiskChecks || []).map((r) => [
      fmt(r.id),
      fmt(r.orderId),
      fmt(r.ruleName),
      `<span class="badge ${r.passed ? 'pass' : 'fail'}">${r.passed ? 'PASS' : 'FAIL'}</span>`,
      fmt(r.message),
      fmtTime(r.checkedAt),
    ]),
  );

  renderTable(
    fillsTable,
    ['ID', 'OrderID', 'Instrument', 'Qty', 'Price', 'FilledAt'],
    (data.recentFills || []).map((f) => [fmt(f.id), fmt(f.orderId), fmt(f.instrumentSymbol), fmt(f.fillQuantity), fmt(f.fillPrice), fmtTime(f.filledAt)]),
  );

  renderTable(
    positionsTable,
    ['ID', 'Strategy', 'Instrument', 'NetQty', 'AvgPrice', 'UpdatedAt'],
    (data.positions || []).map((p) => [fmt(p.id), fmt(p.strategyName), fmt(p.instrumentSymbol), fmt(p.netQuantity), fmt(p.averagePrice), fmtTime(p.updatedAt)]),
  );

  renderTable(
    portfolioTable,
    ['ID', 'StrategyRun', 'Strategy', 'SnapshotAt', 'MarketValue', 'UnrealizedPnL', 'RealizedPnL', 'TotalPnL', 'ReturnRate'],
    (data.recentPortfolioSnapshots || []).map((p) => [
      fmt(p.id),
      fmt(p.strategyRunId),
      fmt(p.strategyName),
      fmtTime(p.snapshotAt),
      fmt(p.totalMarketValue),
      fmtSigned(p.unrealizedPnl),
      fmtSigned(p.realizedPnl),
      fmtSigned(p.totalPnl),
      fmtPercent(p.returnRate),
    ]),
  );
}

async function loadOverview() {
  const response = await fetch('/dashboard/overview?limit=20');
  if (!response.ok) {
    throw new Error(`overview load failed: ${response.status}`);
  }
  const data = await response.json();
  renderOverview(data);
}

async function loadMarketDataStatus() {
  const response = await fetch('/market-data/status');
  if (!response.ok) {
    throw new Error(`market-data status failed: ${response.status}`);
  }
  const data = await response.json();
  renderMarketDataStatus(data);
}

function fillOptions(response) {
  state.options = response;

  strategyRunSelect.innerHTML = '';
  if (!response.strategyRuns?.length) {
    strategyRunSelect.innerHTML = '<option value="">(strategy run 없음)</option>';
  } else {
    for (const s of response.strategyRuns) {
      const option = document.createElement('option');
      option.value = String(s.id);
      option.textContent = `${s.id} - ${s.strategyName} (${fmtTime(s.runAt)})`;
      strategyRunSelect.appendChild(option);
    }
  }

  instrumentSelect.innerHTML = '';
  if (!response.instruments?.length) {
    instrumentSelect.innerHTML = '<option value="">(instrument 없음)</option>';
  } else {
    for (const i of response.instruments) {
      const option = document.createElement('option');
      option.value = String(i.id);
      option.textContent = `${i.id} - ${i.symbol} (${fmt(i.latestClosePrice)})`;
      instrumentSelect.appendChild(option);
    }
  }
}

async function loadOptions() {
  const response = await fetch('/dashboard/options');
  if (!response.ok) {
    throw new Error(`options load failed: ${response.status}`);
  }
  const data = await response.json();
  fillOptions(data);
}

async function createDemoData() {
  const response = await fetch('/dashboard/seed-demo', { method: 'POST' });
  const body = await response.json();
  if (!response.ok) {
    orderResult.textContent = JSON.stringify(body, null, 2);
    throw new Error(`seed failed: ${response.status}`);
  }
  orderResult.textContent = JSON.stringify({ status: response.status, body }, null, 2);
  await Promise.all([loadOptions(), loadOverview(), loadMarketDataStatus()]);
}

async function ingestMarketData() {
  const response = await fetch('/market-data/ingest', { method: 'POST' });
  const body = await response.json();
  marketDataResult.textContent = JSON.stringify({ status: response.status, body }, null, 2);
  await Promise.all([loadOptions(), loadOverview(), loadMarketDataStatus()]);
}

async function refreshPortfolioSnapshots() {
  const response = await fetch('/dashboard/portfolio-snapshots/refresh', { method: 'POST' });
  const body = await response.json();
  orderResult.textContent = JSON.stringify({ status: response.status, body }, null, 2);
  await Promise.all([loadOverview(), loadMarketDataStatus()]);
}

async function submitOrder(evt) {
  evt.preventDefault();

  const formData = new FormData(orderForm);
  const payload = {
    strategyRunId: Number(formData.get('strategyRunId')),
    instrumentId: Number(formData.get('instrumentId')),
    side: formData.get('side'),
    quantity: String(formData.get('quantity')),
    orderType: formData.get('orderType'),
    limitPrice: formData.get('limitPrice') ? String(formData.get('limitPrice')) : null,
    clientOrderId: String(formData.get('clientOrderId')),
  };

  const response = await fetch('/orders', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(payload),
  });

  const body = await response.json().catch(() => ({}));
  orderResult.textContent = JSON.stringify({ status: response.status, body }, null, 2);

  await Promise.all([loadOverview(), loadMarketDataStatus()]);
}

function setAutoRefresh(enabled) {
  if (state.timerId) {
    clearInterval(state.timerId);
    state.timerId = null;
  }
  if (enabled) {
    state.timerId = setInterval(() => {
      Promise.all([loadOverview(), loadMarketDataStatus()]).catch((err) => {
        orderResult.textContent = `refresh error: ${err.message}`;
      });
    }, 5000);
  }
}

refreshBtn.addEventListener('click', () => {
  Promise.all([loadOverview(), loadOptions(), loadMarketDataStatus()]).catch((err) => {
    orderResult.textContent = `refresh error: ${err.message}`;
  });
});

seedBtn.addEventListener('click', () => {
  createDemoData().catch((err) => {
    orderResult.textContent = `seed error: ${err.message}`;
  });
});

ingestBtn.addEventListener('click', () => {
  ingestMarketData().catch((err) => {
    marketDataResult.textContent = `ingest error: ${err.message}`;
  });
});

snapshotBtn.addEventListener('click', () => {
  refreshPortfolioSnapshots().catch((err) => {
    orderResult.textContent = `snapshot error: ${err.message}`;
  });
});

autoRefreshToggle.addEventListener('change', (evt) => {
  setAutoRefresh(evt.target.checked);
});

orderForm.addEventListener('submit', (evt) => {
  submitOrder(evt).catch((err) => {
    orderResult.textContent = `order submit error: ${err.message}`;
  });
});

(async () => {
  try {
    await Promise.all([loadOptions(), loadOverview(), loadMarketDataStatus()]);
    setAutoRefresh(true);
  } catch (err) {
    orderResult.textContent = `initial load error: ${err.message}`;
  }
})();
