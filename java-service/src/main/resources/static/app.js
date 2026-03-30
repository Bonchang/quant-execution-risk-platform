const state = {
  timerId: null,
  options: { strategyRuns: [], instruments: [] },
};

const summaryCards = document.getElementById('summary-cards');
const statusCards = document.getElementById('status-cards');
const ordersTable = document.getElementById('orders-table');
const riskTable = document.getElementById('risk-table');
const fillsTable = document.getElementById('fills-table');
const positionsTable = document.getElementById('positions-table');
const eventFeed = document.getElementById('event-feed');
const orderResult = document.getElementById('order-result');
const orderForm = document.getElementById('order-form');
const strategyRunSelect = document.getElementById('strategy-run-select');
const instrumentSelect = document.getElementById('instrument-select');
const refreshBtn = document.getElementById('refresh-btn');
const seedBtn = document.getElementById('seed-btn');
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
    ? statuses
      .map((status) => `<div class="status"><div class="name">${status}</div><div class="count">${counts[status]}</div></div>`)
      .join('')
    : '<div class="status"><div class="name">ORDERS</div><div class="count">0</div></div>';
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

function renderOverview(data) {
  renderSummary(data.summary || { totalOrders: 0, filledOrders: 0, rejectedOrders: 0, fillRatePercent: 0, rejectionRatePercent: 0 });
  renderStatusCounts(data.statusCounts || {});
  renderEventFeed(data);

  renderTable(
    ordersTable,
    ['ID', 'ClientOrderId', 'Status', 'Side', 'Qty', 'Type', 'Instrument', 'Strategy', 'CreatedAt'],
    (data.recentOrders || []).map((o) => [
      fmt(o.id), fmt(o.clientOrderId), fmt(o.status), fmt(o.side), fmt(o.quantity), fmt(o.orderType), fmt(o.instrumentSymbol), fmt(o.strategyName), fmtTime(o.createdAt),
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
}

async function loadOverview() {
  const response = await fetch('/dashboard/overview?limit=20');
  if (!response.ok) {
    throw new Error(`overview load failed: ${response.status}`);
  }
  const data = await response.json();
  renderOverview(data);
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
  await loadOptions();
  await loadOverview();
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
    clientOrderId: String(formData.get('clientOrderId')),
  };

  const response = await fetch('/orders', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(payload),
  });

  const body = await response.json().catch(() => ({}));
  orderResult.textContent = JSON.stringify({ status: response.status, body }, null, 2);

  await loadOverview();
}

function setAutoRefresh(enabled) {
  if (state.timerId) {
    clearInterval(state.timerId);
    state.timerId = null;
  }
  if (enabled) {
    state.timerId = setInterval(() => {
      loadOverview().catch((err) => {
        orderResult.textContent = `overview refresh error: ${err.message}`;
      });
    }, 5000);
  }
}

refreshBtn.addEventListener('click', () => {
  Promise.all([loadOverview(), loadOptions()]).catch((err) => {
    orderResult.textContent = `refresh error: ${err.message}`;
  });
});

seedBtn.addEventListener('click', () => {
  createDemoData().catch((err) => {
    orderResult.textContent = `seed error: ${err.message}`;
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
    await loadOptions();
    await loadOverview();
    setAutoRefresh(true);
  } catch (err) {
    orderResult.textContent = `initial load error: ${err.message}`;
  }
})();
