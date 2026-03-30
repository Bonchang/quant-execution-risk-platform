const state = {
  timerId: null,
};

const statusCards = document.getElementById('status-cards');
const ordersTable = document.getElementById('orders-table');
const riskTable = document.getElementById('risk-table');
const fillsTable = document.getElementById('fills-table');
const positionsTable = document.getElementById('positions-table');
const orderResult = document.getElementById('order-result');
const orderForm = document.getElementById('order-form');
const refreshBtn = document.getElementById('refresh-btn');
const autoRefreshToggle = document.getElementById('auto-refresh');

function fmt(value) {
  if (value === null || value === undefined) return '-';
  return String(value);
}

function fmtTime(value) {
  if (!value) return '-';
  return value.replace('T', ' ');
}

function renderTable(tableEl, headers, rows) {
  const head = `<thead><tr>${headers.map((h) => `<th>${h}</th>`).join('')}</tr></thead>`;
  const bodyRows = rows.length
    ? rows.map((row) => `<tr>${row.map((c) => `<td>${c}</td>`).join('')}</tr>`).join('')
    : `<tr><td colspan="${headers.length}">데이터 없음</td></tr>`;
  tableEl.innerHTML = `${head}<tbody>${bodyRows}</tbody>`;
}

function renderOverview(data) {
  const counts = data.statusCounts || {};
  const statuses = Object.keys(counts).sort();
  statusCards.innerHTML = statuses.length
    ? statuses.map((status) => `<div class="card"><div class="label">${status}</div><div class="value">${counts[status]}</div></div>`).join('')
    : '<div class="card"><div class="label">주문</div><div class="value">0</div></div>';

  renderTable(
    ordersTable,
    ['ID', 'ClientOrderId', 'Status', 'Side', 'Qty', 'Type', 'Instrument', 'Strategy', 'CreatedAt'],
    (data.recentOrders || []).map((o) => [fmt(o.id), fmt(o.clientOrderId), fmt(o.status), fmt(o.side), fmt(o.quantity), fmt(o.orderType), fmt(o.instrumentSymbol), fmt(o.strategyName), fmtTime(o.createdAt)])
  );

  renderTable(
    riskTable,
    ['ID', 'OrderID', 'Rule', 'Passed', 'Message', 'CheckedAt'],
    (data.recentRiskChecks || []).map((r) => [fmt(r.id), fmt(r.orderId), fmt(r.ruleName), `<span class="badge ${r.passed ? 'pass' : 'fail'}">${r.passed ? 'PASS' : 'FAIL'}</span>`, fmt(r.message), fmtTime(r.checkedAt)])
  );

  renderTable(
    fillsTable,
    ['ID', 'OrderID', 'Instrument', 'Qty', 'Price', 'FilledAt'],
    (data.recentFills || []).map((f) => [fmt(f.id), fmt(f.orderId), fmt(f.instrumentSymbol), fmt(f.fillQuantity), fmt(f.fillPrice), fmtTime(f.filledAt)])
  );

  renderTable(
    positionsTable,
    ['ID', 'Strategy', 'Instrument', 'NetQty', 'AvgPrice', 'UpdatedAt'],
    (data.positions || []).map((p) => [fmt(p.id), fmt(p.strategyName), fmt(p.instrumentSymbol), fmt(p.netQuantity), fmt(p.averagePrice), fmtTime(p.updatedAt)])
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

  const body = await response.text();
  let parsed;
  try {
    parsed = JSON.parse(body);
  } catch {
    parsed = body;
  }

  orderResult.textContent = JSON.stringify({ status: response.status, body: parsed }, null, 2);
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
  loadOverview().catch((err) => {
    orderResult.textContent = `overview load error: ${err.message}`;
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

loadOverview().catch((err) => {
  orderResult.textContent = `overview load error: ${err.message}`;
});
setAutoRefresh(true);
