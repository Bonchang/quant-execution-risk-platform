import type {
  AppMeDto,
  AuthTokenResponse,
  DiscoverScreenDto,
  DashboardOptionsDto,
  DashboardOverviewDto,
  DashboardTimelineDto,
  HomeScreenDto,
  MarketDataHealthDto,
  MarketDataStatusDto,
  MarketQuoteDto,
  OrdersScreenDto,
  OrderDetailDto,
  OrderSummaryDto,
  PortfolioScreenDto,
  QuantOverviewDto,
  QuantStrategyDetailDto,
  ResearchRunDetailDto,
  ResearchRunSummaryDto,
  StockDetailDto,
} from '../types/api';

interface RequestOptions extends RequestInit {
  token?: string;
  onUnauthorized?: () => void;
}

async function request<T>(path: string, options: RequestOptions = {}): Promise<T> {
  const headers = new Headers(options.headers ?? {});
  if (!headers.has('Accept')) {
    headers.set('Accept', 'application/json');
  }
  if (options.token) {
    headers.set('Authorization', `Bearer ${options.token}`);
  }
  if (options.body && !headers.has('Content-Type')) {
    headers.set('Content-Type', 'application/json');
  }

  const response = await fetch(path, {
    ...options,
    headers,
  });

  if (response.status === 401 && options.onUnauthorized) {
    options.onUnauthorized();
  }

  const text = await response.text();
  const body = text ? JSON.parse(text) : {};
  if (!response.ok) {
    throw new Error(body.message ?? body.error ?? `${response.status} ${response.statusText}`);
  }
  return body as T;
}

export const apiClient = {
  getAppHome(token?: string, onUnauthorized?: () => void) {
    return request<HomeScreenDto>('/app/home', { token, onUnauthorized });
  },
  getDiscover() {
    return request<DiscoverScreenDto>('/app/discover');
  },
  getStock(symbol: string, token?: string, onUnauthorized?: () => void) {
    return request<StockDetailDto>(`/app/stocks/${symbol}`, { token, onUnauthorized });
  },
  getPortfolio(token: string, onUnauthorized: () => void) {
    return request<PortfolioScreenDto>('/app/portfolio', { token, onUnauthorized });
  },
  getConsumerOrders(token: string, onUnauthorized: () => void) {
    return request<OrdersScreenDto>('/app/orders', { token, onUnauthorized });
  },
  getConsumerOrder(id: string, token: string, onUnauthorized: () => void) {
    return request<OrderDetailDto>(`/app/orders/${id}`, { token, onUnauthorized });
  },
  createConsumerOrder(payload: Record<string, unknown>, token: string, onUnauthorized: () => void) {
    return request('/app/orders', {
      method: 'POST',
      body: JSON.stringify(payload),
      token,
      onUnauthorized,
    });
  },
  cancelConsumerOrder(id: number, token: string, onUnauthorized: () => void) {
    return request(`/app/orders/${id}/cancel`, { method: 'POST', token, onUnauthorized });
  },
  getQuantOverview() {
    return request<QuantOverviewDto>('/app/quant/overview');
  },
  getQuantStrategy(runId: string) {
    return request<QuantStrategyDetailDto>(`/app/quant/strategies/${runId}`);
  },
  startGuestSession() {
    return request<AuthTokenResponse>('/app/auth/guest', { method: 'POST' });
  },
  getMe(token: string, onUnauthorized: () => void) {
    return request<AppMeDto>('/app/me', { token, onUnauthorized });
  },
  login(username: string, password: string) {
    return request<AuthTokenResponse>('/auth/token', {
      method: 'POST',
      body: JSON.stringify({ username, password }),
    });
  },
  getOverview(token: string, onUnauthorized: () => void) {
    return request<DashboardOverviewDto>('/dashboard/overview?limit=20', { token, onUnauthorized });
  },
  getTimeline(token: string, onUnauthorized: () => void) {
    return request<DashboardTimelineDto>('/dashboard/timeline?limit=50', { token, onUnauthorized });
  },
  getOptions(token: string, onUnauthorized: () => void) {
    return request<DashboardOptionsDto>('/dashboard/options', { token, onUnauthorized });
  },
  listOrders(token: string, onUnauthorized: () => void) {
    return request<OrderSummaryDto[]>('/orders?limit=30', { token, onUnauthorized });
  },
  getOrder(id: string, token: string, onUnauthorized: () => void) {
    return request<OrderDetailDto>(`/orders/${id}`, { token, onUnauthorized });
  },
  cancelOrder(id: number, token: string, onUnauthorized: () => void) {
    return request(`/orders/${id}/cancel`, { method: 'POST', token, onUnauthorized });
  },
  createOrder(payload: Record<string, unknown>, token: string, onUnauthorized: () => void) {
    return request('/orders', {
      method: 'POST',
      body: JSON.stringify(payload),
      token,
      onUnauthorized,
    });
  },
  seedDemo(token: string, onUnauthorized: () => void) {
    return request('/dashboard/seed-demo', { method: 'POST', token, onUnauthorized });
  },
  refreshSnapshots(token: string, onUnauthorized: () => void) {
    return request('/dashboard/portfolio-snapshots/refresh', { method: 'POST', token, onUnauthorized });
  },
  ingestMarketData(token: string, onUnauthorized: () => void) {
    return request('/market-data/ingest', { method: 'POST', token, onUnauthorized });
  },
  getMarketStatus(token: string, onUnauthorized: () => void) {
    return request<MarketDataStatusDto>('/market-data/status', { token, onUnauthorized });
  },
  getMarketHealth(token: string, onUnauthorized: () => void) {
    return request<MarketDataHealthDto>('/market-data/health', { token, onUnauthorized });
  },
  getQuotes(token: string, onUnauthorized: () => void) {
    return request<MarketQuoteDto[]>('/market-data/quotes', { token, onUnauthorized });
  },
  listResearchRuns() {
    return request<ResearchRunSummaryDto[]>('/research/runs');
  },
  getResearchRun(runId: string) {
    return request<ResearchRunDetailDto>(`/research/runs/${runId}`);
  },
};
