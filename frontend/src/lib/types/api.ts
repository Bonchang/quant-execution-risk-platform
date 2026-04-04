export type Role = 'ROLE_ADMIN' | 'ROLE_TRADER' | 'ROLE_VIEWER' | 'ROLE_GUEST' | '';

export interface AuthTokenResponse {
  accessToken: string;
  tokenType: string;
  expiresInSeconds: number;
  role: Role;
  authType?: string;
  userId?: number;
  accountId?: number;
  displayName?: string;
  accountCode?: string;
}

export interface AppMeDto {
  userId: number;
  authType: string;
  role: Role;
  displayName: string;
  account: {
    accountId: number;
    accountCode: string;
    ownerName: string;
    baseCurrency: string;
    availableCash: number;
    reservedCash: number;
  };
  marketConnection: {
    status: string;
    source: string;
    stale: boolean;
    staleQuoteCount: number;
    lastQuoteReceivedAt: string | null;
  };
}

export interface DashboardOverviewDto {
  summary: {
    totalOrders: number;
    filledOrders: number;
    rejectedOrders: number;
    fillRatePercent: number;
    rejectionRatePercent: number;
  };
  portfolioSummary: {
    strategyRunId: number | null;
    accountId: number | null;
    accountCode: string | null;
    strategyName: string | null;
    snapshotAt: string | null;
    totalMarketValue: number | null;
    unrealizedPnl: number | null;
    realizedPnl: number | null;
    totalPnl: number | null;
    returnRate: number | null;
  };
  researchSummary: {
    runId: string | null;
    strategyName: string | null;
    instrumentSymbol: string | null;
    generatedAt: string | null;
    metrics: Record<string, unknown>;
  };
  quoteSummary: {
    totalQuotes: number;
    staleQuotes: number;
    lastQuoteReceivedAt: string | null;
    source: string | null;
  };
  marketDataHealth: {
    status: string;
    enabled: boolean;
    apiKeyConfigured: boolean;
    source: string;
    lastRunAt: string | null;
    lastRunStatus: string | null;
    lastQuoteReceivedAt: string | null;
    staleQuoteCount: number;
  };
  statusCounts: Record<string, number>;
  accountSummaries: AccountSummaryDto[];
  recentOrders: OrderSummaryDto[];
  recentRiskChecks: RiskCheckDto[];
  recentFills: FillDto[];
  positions: PositionDto[];
  recentPortfolioSnapshots: PortfolioSnapshotDto[];
  recentQuotes: MarketQuoteDto[];
  recentOutboxEvents: OutboxEventDto[];
}

export interface DashboardTimelineDto {
  events: TimelineEventDto[];
}

export interface TimelineEventDto {
  category: string;
  eventType: string;
  severity: string;
  title: string;
  description: string;
  subjectKey: string;
  occurredAt: string;
}

export interface AccountSummaryDto {
  accountId?: number;
  id?: number;
  accountCode: string;
  ownerName: string;
  baseCurrency: string;
  availableCash: number;
  reservedCash: number;
  updatedAt: string | null;
}

export interface OrderSummaryDto {
  id: number;
  accountId: number;
  accountCode?: string;
  strategyRunId?: number;
  instrumentId: number;
  side: 'BUY' | 'SELL';
  quantity?: number;
  requestedQuantity?: number;
  limitPrice: number | null;
  reservedCashAmount: number;
  filledQuantity: number;
  remainingQuantity: number;
  orderType: 'MARKET' | 'LIMIT';
  timeInForce: 'DAY' | 'GTC';
  status: string;
  clientOrderId: string;
  createdAt?: string;
  expiresAt: string | null;
  lastExecutedAt?: string | null;
  updatedAt: string;
  strategyName?: string;
  instrumentSymbol?: string;
}

export interface RiskCheckDto {
  id: number;
  orderId?: number;
  ruleName: string;
  passed: boolean;
  message: string;
  checkedAt: string;
}

export interface FillDto {
  id: number;
  orderId?: number;
  instrumentSymbol?: string;
  fillQuantity: number;
  fillPrice: number;
  filledAt: string;
}

export interface PositionDto {
  id: number;
  strategyName: string;
  instrumentSymbol: string;
  netQuantity: number;
  averagePrice: number;
  updatedAt: string;
}

export interface PortfolioSnapshotDto {
  id: number;
  strategyRunId: number;
  accountId: number;
  accountCode: string;
  strategyName: string;
  snapshotAt: string;
  totalMarketValue: number;
  unrealizedPnl: number;
  realizedPnl: number;
  totalPnl: number;
  returnRate: number;
}

export interface MarketQuoteDto {
  instrumentId: number;
  symbol: string;
  market: string;
  quoteTime: string;
  lastPrice: number;
  bidPrice: number;
  askPrice: number;
  changePercent: number;
  source: string;
  receivedAt: string;
  stale: boolean;
}

export interface OutboxEventDto {
  id: number;
  aggregateType: string;
  aggregateId: number;
  eventType: string;
  processingStatus: string;
  createdAt: string;
  processedAt: string | null;
}

export interface DashboardOptionsDto {
  accounts: {
    id: number;
    accountCode: string;
    ownerName: string;
    baseCurrency: string;
  }[];
  strategyRuns: {
    id: number;
    accountId: number;
    strategyName: string;
    runAt: string;
  }[];
  instruments: {
    id: number;
    symbol: string;
    name: string;
    market: string;
    latestClosePrice: number | null;
    latestPriceDate: string | null;
  }[];
}

export interface OrderDetailDto {
  id: number;
  accountId: number;
  accountCode: string;
  ownerName: string;
  baseCurrency: string;
  availableCash: number;
  reservedCash: number;
  strategyRunId: number;
  strategyName: string;
  instrumentId: number;
  instrumentSymbol: string;
  side: 'BUY' | 'SELL';
  quantity: number;
  limitPrice: number | null;
  reservedCashAmount: number;
  filledQuantity: number;
  remainingQuantity: number;
  orderType: 'MARKET' | 'LIMIT';
  timeInForce: 'DAY' | 'GTC';
  status: string;
  clientOrderId: string;
  createdAt: string;
  expiresAt: string | null;
  lastExecutedAt: string | null;
  updatedAt: string;
  fills: FillDto[];
  riskChecks: RiskCheckDto[];
  outboxEvents: OutboxEventDto[];
  cashLedgerEntries: {
    id: number;
    entryType: string;
    amount: number;
    availableCashAfter: number;
    reservedCashAfter: number;
    note: string;
    createdAt: string;
  }[];
}

export interface ResearchRunSummaryDto {
  runId: string;
  strategyName: string;
  instrumentSymbol: string;
  generatedAt: string;
  metrics: Record<string, unknown>;
  artifactAvailability: Record<string, boolean>;
  reportPath: string;
}

export interface ResearchRunDetailDto extends ResearchRunSummaryDto {
  config: Record<string, unknown>;
  artifactFiles: Record<string, string>;
  equityCurveRows: Record<string, unknown>[];
  tradeRows: Record<string, unknown>[];
  signalRows: Record<string, unknown>[];
}

export interface MarketDataStatusDto {
  enabled: boolean;
  apiKeyConfigured: boolean;
  lastRunAt: string | null;
  lastQuoteReceivedAt: string | null;
  source: string;
  stale: boolean;
  staleQuoteCount: number;
  lastResult: {
    totalInstruments: number;
    successCount: number;
    failureCount: number;
    runStatus: string;
    failures: string[];
  } | null;
}

export interface MarketDataHealthDto {
  status: string;
  enabled: boolean;
  apiKeyConfigured: boolean;
  source: string;
  lastRunAt: string | null;
  lastRunStatus: string;
  lastQuoteReceivedAt: string | null;
  totalQuotes: number;
  staleQuoteCount: number;
  staleSymbols: string[];
  recentFailures: string[];
}

export interface HomeScreenDto {
  guestAvailable: boolean;
  assetSummary: {
    accountId: number | null;
    accountCode: string;
    ownerName: string;
    baseCurrency: string;
    totalAssets: number;
    investedAmount: number;
    cashAmount: number;
    totalPnl: number;
    returnRate: number;
    snapshotAt: string | null;
  };
  marketConnection: {
    status: string;
    source: string;
    stale: boolean;
    staleQuoteCount: number;
    lastQuoteReceivedAt: string | null;
  };
  highlights: {
    title: string;
    body: string;
    tone: string;
  }[];
  featuredStocks: {
    symbol: string;
    name: string;
    market: string;
    lastPrice: number;
    changePercent: number;
    stale: boolean;
    reason: string;
  }[];
  quantSpotlight: {
    runId: string;
    strategyName: string;
    instrumentSymbol: string;
    generatedAt: string;
    metrics: Record<string, unknown>;
    signalHeadline: string;
    signalStrength: number;
  } | null;
}

export interface DiscoverScreenDto {
  marketSummary: {
    marketStatus: string;
    liveQuoteCount: number;
    staleQuoteCount: number;
    lastQuoteReceivedAt: string | null;
    source: string;
    topMoverSymbol: string | null;
    topMoverChangePercent: number | null;
  };
  stocks: {
    instrumentId: number;
    symbol: string;
    name: string;
    market: string;
    lastPrice: number;
    bidPrice: number;
    askPrice: number;
    changePercent: number;
    stale: boolean;
  }[];
}

export interface StockDetailDto {
  stock: {
    instrumentId: number;
    symbol: string;
    name: string;
    market: string;
    lastPrice: number;
    bidPrice: number;
    askPrice: number;
    changePercent: number;
    stale: boolean;
    receivedAt: string | null;
    marketStatus: string;
  };
  priceSeries: {
    date: string;
    closePrice: number;
  }[];
  quantInsight: {
    strategyName: string;
    headline: string;
    summary: string;
    trendLabel: string;
    volatilityLabel: string;
    signalStrength: number;
    metrics: Record<string, unknown>;
    reasons: string[];
  };
  riskSummary: {
    availableCash: number;
    estimatedBuyPrice: number;
    estimatedSellPrice: number;
    maxAffordableQuantity: number;
    staleQuote: boolean;
    staleMessage: string;
    executionHint: string;
  };
  tradeContext: {
    accountId: number;
    accountCode: string;
    strategyRunId: number;
    strategyName: string;
    availableCash: number;
  } | null;
  recentOrders: ActivityItemDto[];
  recentExecutions: ActivityItemDto[];
}

export interface ActivityItemDto {
  type: string;
  title: string;
  status: string;
  quantity: number;
  price: number | null;
  occurredAt: string;
}

export interface PortfolioScreenDto {
  assetSummary: {
    totalAssets: number;
    cashAmount: number;
    investedAmount: number;
    totalPnl: number;
    returnRate: number;
    snapshotAt: string | null;
  };
  account: {
    accountId: number | null;
    accountCode: string;
    ownerName: string;
    baseCurrency: string;
    availableCash: number;
    reservedCash: number;
  };
  holdings: {
    symbol: string;
    strategyName: string;
    netQuantity: number;
    averagePrice: number;
    lastPrice: number;
    marketValue: number;
    unrealizedPnl: number;
  }[];
  assetTrend: {
    snapshotAt: string;
    totalAssets: number;
    totalPnl: number;
  }[];
  recentExecutions: {
    orderId: number;
    symbol: string;
    fillQuantity: number;
    fillPrice: number;
    filledAt: string;
  }[];
}

export interface OrdersScreenDto {
  summary: {
    totalOrders: number;
    filledOrders: number;
    workingOrders: number;
    rejectedOrders: number;
  };
  orders: {
    id: number;
    symbol: string;
    side: string;
    status: string;
    quantity: number;
    filledQuantity: number;
    limitPrice: number | null;
    orderType: string;
    accountCode: string;
    updatedAt: string;
    cancelable: boolean;
  }[];
}

export interface QuantOverviewDto {
  featuredInsight: {
    runId: string;
    strategyName: string;
    instrumentSymbol: string;
    generatedAt: string;
    headline: string;
    signalStrength: number;
    metrics: Record<string, unknown>;
  } | null;
  strategies: {
    runId: string;
    strategyName: string;
    instrumentSymbol: string;
    generatedAt: string;
    lastPrice: number | null;
    changePercent: number | null;
    metrics: Record<string, unknown>;
  }[];
}

export interface QuantStrategyDetailDto {
  runId: string;
  strategyName: string;
  instrumentSymbol: string;
  generatedAt: string;
  metrics: Record<string, unknown>;
  config: Record<string, unknown>;
  artifactAvailability: Record<string, boolean>;
  equityCurveRows: Record<string, unknown>[];
  tradeRows: Record<string, unknown>[];
  signalRows: Record<string, unknown>[];
  linkedInstrument: {
    symbol: string;
    name: string;
    market: string;
    lastPrice: number;
    changePercent: number;
    stale: boolean;
  } | null;
  recentOrders: {
    orderId: number;
    status: string;
    side: string;
    quantity: number;
    limitPrice: number | null;
    updatedAt: string;
  }[];
}
