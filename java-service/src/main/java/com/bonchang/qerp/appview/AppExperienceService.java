package com.bonchang.qerp.appview;

import com.bonchang.qerp.marketdata.MarketDataHealthResponse;
import com.bonchang.qerp.marketdata.MarketDataProperties;
import com.bonchang.qerp.marketdata.MarketDataStatusService;
import com.bonchang.qerp.research.ResearchArtifactService;
import com.bonchang.qerp.research.ResearchRunDetailResponse;
import com.bonchang.qerp.research.ResearchRunSummaryResponse;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AppExperienceService {

    private final JdbcTemplate jdbcTemplate;
    private final ResearchArtifactService researchArtifactService;
    private final MarketDataStatusService marketDataStatusService;
    private final MarketDataProperties marketDataProperties;

    public HomeScreenResponse loadHome() {
        HomeScreenResponse.AssetSummary assetSummary = loadHomeAssetSummary();
        MarketDataHealthResponse health = marketHealth();
        HomeScreenResponse.MarketConnection marketConnection = new HomeScreenResponse.MarketConnection(
                health.status(),
                health.source(),
                health.staleQuoteCount() > 0,
                health.staleQuoteCount(),
                health.lastQuoteReceivedAt()
        );
        ResearchRunSummaryResponse latestRun = researchArtifactService.latestRun();
        List<HomeScreenResponse.Highlight> highlights = buildHighlights(assetSummary, marketConnection, latestRun);

        return new HomeScreenResponse(
                assetSummary,
                marketConnection,
                highlights,
                loadFeaturedStocks(),
                latestRun != null ? toQuantSpotlight(latestRun) : null
        );
    }

    public DiscoverScreenResponse loadDiscover() {
        List<DiscoverScreenResponse.StockCard> stocks = jdbcTemplate.query(
                """
                SELECT i.id,
                       i.symbol,
                       i.name,
                       i.market,
                       mq.last_price,
                       mq.bid_price,
                       mq.ask_price,
                       mq.change_percent,
                       mq.received_at < ? AS stale
                FROM instrument i
                JOIN market_quote mq ON mq.instrument_id = i.id
                ORDER BY ABS(mq.change_percent) DESC, i.symbol ASC
                LIMIT 24
                """,
                stockCardMapper(),
                staleThreshold()
        );

        DiscoverScreenResponse.StockCard topMover = stocks.stream().findFirst().orElse(null);
        MarketDataHealthResponse health = marketHealth();
        return new DiscoverScreenResponse(
                new DiscoverScreenResponse.MarketSummary(
                        health.status(),
                        health.totalQuotes() - health.staleQuoteCount(),
                        health.staleQuoteCount(),
                        health.lastQuoteReceivedAt(),
                        health.source(),
                        topMover != null ? topMover.symbol() : null,
                        topMover != null ? topMover.changePercent() : null
                ),
                stocks
        );
    }

    public StockDetailResponse loadStock(String symbol) {
        StockDetailResponse.StockHeader stock = jdbcTemplate.query(
                """
                SELECT i.id,
                       i.symbol,
                       i.name,
                       i.market,
                       COALESCE(mq.last_price, mp.close_price) AS last_price,
                       COALESCE(mq.bid_price, mp.close_price) AS bid_price,
                       COALESCE(mq.ask_price, mp.close_price) AS ask_price,
                       COALESCE(mq.change_percent, 0) AS change_percent,
                       mq.received_at < ? AS stale,
                       mq.received_at,
                       CASE
                           WHEN mq.received_at IS NULL THEN 'NO_QUOTE'
                           WHEN mq.received_at < ? THEN 'STALE'
                           ELSE 'LIVE'
                       END AS market_status
                FROM instrument i
                LEFT JOIN market_quote mq ON mq.instrument_id = i.id
                LEFT JOIN LATERAL (
                    SELECT close_price
                    FROM market_price mp
                    WHERE mp.instrument_id = i.id
                    ORDER BY mp.price_date DESC, mp.id DESC
                    LIMIT 1
                ) mp ON true
                WHERE UPPER(i.symbol) = UPPER(?)
                """,
                (rs, rowNum) -> new StockDetailResponse.StockHeader(
                        rs.getLong("id"),
                        rs.getString("symbol"),
                        rs.getString("name"),
                        rs.getString("market"),
                        rs.getBigDecimal("last_price"),
                        rs.getBigDecimal("bid_price"),
                        rs.getBigDecimal("ask_price"),
                        rs.getBigDecimal("change_percent"),
                        rs.getBoolean("stale"),
                        toDateTime(rs.getTimestamp("received_at")),
                        rs.getString("market_status")
                ),
                staleThreshold(),
                staleThreshold(),
                symbol
        ).stream().findFirst().orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "symbol not found"));

        ResearchRunSummaryResponse researchSummary = findLatestRunBySymbol(stock.symbol());
        ResearchRunDetailResponse researchDetail = researchSummary != null
                ? researchArtifactService.getRun(researchSummary.runId())
                : null;

        return new StockDetailResponse(
                stock,
                loadPriceSeries(stock.instrumentId()),
                buildQuantInsight(stock.symbol(), researchDetail),
                buildRiskSummary(stock),
                loadTradeContext(stock.instrumentId()),
                loadRecentOrdersForSymbol(stock.symbol()),
                loadRecentExecutionsForSymbol(stock.symbol())
        );
    }

    public PortfolioScreenResponse loadPortfolio() {
        PortfolioScreenResponse.AccountState account = jdbcTemplate.query(
                """
                SELECT a.id,
                       a.account_code,
                       a.owner_name,
                       a.base_currency,
                       cb.available_cash,
                       cb.reserved_cash
                FROM account a
                JOIN cash_balance cb ON cb.account_id = a.id
                ORDER BY a.id
                LIMIT 1
                """,
                (rs, rowNum) -> new PortfolioScreenResponse.AccountState(
                        rs.getLong("id"),
                        rs.getString("account_code"),
                        rs.getString("owner_name"),
                        rs.getString("base_currency"),
                        rs.getBigDecimal("available_cash"),
                        rs.getBigDecimal("reserved_cash")
                )
        ).stream().findFirst().orElse(new PortfolioScreenResponse.AccountState(null, "-", "-", "USD", BigDecimal.ZERO, BigDecimal.ZERO));

        PortfolioScreenResponse.AssetSummary assetSummary = jdbcTemplate.query(
                """
                SELECT ps.snapshot_at,
                       ps.total_market_value,
                       ps.total_pnl,
                       ps.return_rate
                FROM portfolio_snapshot ps
                ORDER BY ps.snapshot_at DESC, ps.id DESC
                LIMIT 1
                """,
                (rs, rowNum) -> {
                    BigDecimal investedAmount = defaultDecimal(rs.getBigDecimal("total_market_value"));
                    BigDecimal cashAmount = defaultDecimal(account.availableCash()).add(defaultDecimal(account.reservedCash()));
                    return new PortfolioScreenResponse.AssetSummary(
                            investedAmount.add(cashAmount),
                            cashAmount,
                            investedAmount,
                            defaultDecimal(rs.getBigDecimal("total_pnl")),
                            defaultDecimal(rs.getBigDecimal("return_rate")),
                            rs.getTimestamp("snapshot_at").toLocalDateTime()
                    );
                }
        ).stream().findFirst().orElseGet(() -> new PortfolioScreenResponse.AssetSummary(
                defaultDecimal(account.availableCash()).add(defaultDecimal(account.reservedCash())),
                defaultDecimal(account.availableCash()).add(defaultDecimal(account.reservedCash())),
                BigDecimal.ZERO.setScale(6, RoundingMode.HALF_UP),
                BigDecimal.ZERO.setScale(6, RoundingMode.HALF_UP),
                BigDecimal.ZERO.setScale(6, RoundingMode.HALF_UP),
                null
        ));

        List<PortfolioScreenResponse.HoldingItem> holdings = jdbcTemplate.query(
                """
                SELECT i.symbol,
                       s.strategy_name,
                       p.net_quantity,
                       p.average_price,
                       COALESCE(mq.last_price, mp.close_price, p.average_price) AS last_price
                FROM position p
                JOIN instrument i ON i.id = p.instrument_id
                JOIN strategy_run s ON s.id = p.strategy_run_id
                LEFT JOIN market_quote mq ON mq.instrument_id = p.instrument_id
                LEFT JOIN LATERAL (
                    SELECT close_price
                    FROM market_price mpx
                    WHERE mpx.instrument_id = p.instrument_id
                    ORDER BY mpx.price_date DESC, mpx.id DESC
                    LIMIT 1
                ) mp ON true
                WHERE p.net_quantity <> 0
                ORDER BY ABS(p.net_quantity) DESC, i.symbol ASC
                """,
                (rs, rowNum) -> {
                    BigDecimal quantity = rs.getBigDecimal("net_quantity");
                    BigDecimal lastPrice = defaultDecimal(rs.getBigDecimal("last_price"));
                    BigDecimal averagePrice = defaultDecimal(rs.getBigDecimal("average_price"));
                    BigDecimal marketValue = lastPrice.multiply(quantity);
                    BigDecimal unrealized = lastPrice.subtract(averagePrice).multiply(quantity);
                    return new PortfolioScreenResponse.HoldingItem(
                            rs.getString("symbol"),
                            rs.getString("strategy_name"),
                            quantity,
                            averagePrice,
                            lastPrice,
                            marketValue,
                            unrealized
                    );
                }
        );

        List<PortfolioScreenResponse.TrendPoint> trend = jdbcTemplate.query(
                """
                SELECT snapshot_at,
                       total_market_value,
                       total_pnl
                FROM portfolio_snapshot
                ORDER BY snapshot_at DESC, id DESC
                LIMIT 12
                """,
                (rs, rowNum) -> new PortfolioScreenResponse.TrendPoint(
                        rs.getTimestamp("snapshot_at").toLocalDateTime(),
                        defaultDecimal(rs.getBigDecimal("total_market_value")).add(defaultDecimal(account.availableCash())).add(defaultDecimal(account.reservedCash())),
                        defaultDecimal(rs.getBigDecimal("total_pnl"))
                )
        ).stream().sorted(Comparator.comparing(PortfolioScreenResponse.TrendPoint::snapshotAt)).toList();

        List<PortfolioScreenResponse.ExecutionItem> recentExecutions = jdbcTemplate.query(
                """
                SELECT f.order_id,
                       i.symbol,
                       f.fill_quantity,
                       f.fill_price,
                       f.filled_at
                FROM fill f
                JOIN instrument i ON i.id = f.instrument_id
                ORDER BY f.filled_at DESC, f.id DESC
                LIMIT 8
                """,
                (rs, rowNum) -> new PortfolioScreenResponse.ExecutionItem(
                        rs.getLong("order_id"),
                        rs.getString("symbol"),
                        rs.getBigDecimal("fill_quantity"),
                        rs.getBigDecimal("fill_price"),
                        rs.getTimestamp("filled_at").toLocalDateTime()
                )
        );

        return new PortfolioScreenResponse(assetSummary, account, holdings, trend, recentExecutions);
    }

    public OrdersScreenResponse loadOrders() {
        Map<String, Long> counts = new LinkedHashMap<>();
        jdbcTemplate.queryForList("SELECT status, COUNT(*) AS cnt FROM orders GROUP BY status")
                .forEach(row -> counts.put(String.valueOf(row.get("status")), ((Number) row.get("cnt")).longValue()));

        List<OrdersScreenResponse.OrderItem> orders = jdbcTemplate.query(
                """
                SELECT o.id,
                       i.symbol,
                       o.side,
                       o.status,
                       o.quantity,
                       o.filled_quantity,
                       o.limit_price,
                       o.order_type,
                       a.account_code,
                       o.updated_at
                FROM orders o
                JOIN instrument i ON i.id = o.instrument_id
                JOIN account a ON a.id = o.account_id
                ORDER BY o.updated_at DESC, o.id DESC
                LIMIT 40
                """,
                (rs, rowNum) -> new OrdersScreenResponse.OrderItem(
                        rs.getLong("id"),
                        rs.getString("symbol"),
                        rs.getString("side"),
                        rs.getString("status"),
                        rs.getBigDecimal("quantity"),
                        rs.getBigDecimal("filled_quantity"),
                        rs.getBigDecimal("limit_price"),
                        rs.getString("order_type"),
                        rs.getString("account_code"),
                        rs.getTimestamp("updated_at").toLocalDateTime(),
                        "WORKING".equals(rs.getString("status"))
                )
        );

        return new OrdersScreenResponse(
                new OrdersScreenResponse.Summary(
                        orders.size(),
                        counts.getOrDefault("FILLED", 0L),
                        counts.getOrDefault("WORKING", 0L),
                        counts.getOrDefault("REJECTED", 0L)
                ),
                orders
        );
    }

    public QuantOverviewResponse loadQuantOverview() {
        List<ResearchRunSummaryResponse> runs = researchArtifactService.listRuns().stream().limit(6).toList();
        ResearchRunSummaryResponse latest = runs.stream().findFirst().orElse(null);

        List<QuantOverviewResponse.StrategyCard> strategies = runs.stream()
                .map(run -> new QuantOverviewResponse.StrategyCard(
                        run.runId(),
                        run.strategyName(),
                        run.instrumentSymbol(),
                        run.generatedAt(),
                        latestPriceForSymbol(run.instrumentSymbol()),
                        latestChangeForSymbol(run.instrumentSymbol()),
                        run.metrics()
                ))
                .toList();

        return new QuantOverviewResponse(
                latest != null ? toFeaturedInsight(latest) : null,
                strategies
        );
    }

    public QuantStrategyDetailResponse loadQuantStrategy(String runId) {
        ResearchRunDetailResponse detail = researchArtifactService.getRun(runId);
        return new QuantStrategyDetailResponse(
                detail.runId(),
                detail.strategyName(),
                detail.instrumentSymbol(),
                detail.generatedAt(),
                detail.metrics(),
                detail.config(),
                detail.artifactAvailability(),
                detail.equityCurveRows(),
                detail.tradeRows(),
                detail.signalRows(),
                loadLinkedInstrument(detail.instrumentSymbol()),
                loadRecentOrderFeed(detail.instrumentSymbol())
        );
    }

    private HomeScreenResponse.AssetSummary loadHomeAssetSummary() {
        return jdbcTemplate.query(
                """
                SELECT a.id,
                       a.account_code,
                       a.owner_name,
                       a.base_currency,
                       cb.available_cash,
                       cb.reserved_cash,
                       ps.total_market_value,
                       ps.total_pnl,
                       ps.return_rate,
                       ps.snapshot_at
                FROM account a
                JOIN cash_balance cb ON cb.account_id = a.id
                LEFT JOIN LATERAL (
                    SELECT total_market_value, total_pnl, return_rate, snapshot_at
                    FROM portfolio_snapshot ps
                    WHERE ps.account_id = a.id
                    ORDER BY ps.snapshot_at DESC, ps.id DESC
                    LIMIT 1
                ) ps ON true
                ORDER BY a.id
                LIMIT 1
                """,
                (rs, rowNum) -> {
                    BigDecimal cashAmount = defaultDecimal(rs.getBigDecimal("available_cash")).add(defaultDecimal(rs.getBigDecimal("reserved_cash")));
                    BigDecimal investedAmount = defaultDecimal(rs.getBigDecimal("total_market_value"));
                    return new HomeScreenResponse.AssetSummary(
                            rs.getLong("id"),
                            rs.getString("account_code"),
                            rs.getString("owner_name"),
                            rs.getString("base_currency"),
                            cashAmount.add(investedAmount),
                            investedAmount,
                            cashAmount,
                            defaultDecimal(rs.getBigDecimal("total_pnl")),
                            defaultDecimal(rs.getBigDecimal("return_rate")),
                            toDateTime(rs.getTimestamp("snapshot_at"))
                    );
                }
        ).stream().findFirst().orElse(new HomeScreenResponse.AssetSummary(
                null,
                "-",
                "데모 계좌 없음",
                "USD",
                BigDecimal.ZERO.setScale(6, RoundingMode.HALF_UP),
                BigDecimal.ZERO.setScale(6, RoundingMode.HALF_UP),
                BigDecimal.ZERO.setScale(6, RoundingMode.HALF_UP),
                BigDecimal.ZERO.setScale(6, RoundingMode.HALF_UP),
                BigDecimal.ZERO.setScale(6, RoundingMode.HALF_UP),
                null
        ));
    }

    private List<HomeScreenResponse.FeaturedStock> loadFeaturedStocks() {
        return jdbcTemplate.query(
                """
                SELECT i.symbol,
                       i.name,
                       i.market,
                       mq.last_price,
                       mq.change_percent,
                       mq.received_at < ? AS stale
                FROM market_quote mq
                JOIN instrument i ON i.id = mq.instrument_id
                ORDER BY ABS(mq.change_percent) DESC, mq.received_at DESC
                LIMIT 6
                """,
                (rs, rowNum) -> new HomeScreenResponse.FeaturedStock(
                        rs.getString("symbol"),
                        rs.getString("name"),
                        rs.getString("market"),
                        rs.getBigDecimal("last_price"),
                        rs.getBigDecimal("change_percent"),
                        rs.getBoolean("stale"),
                        rs.getBigDecimal("change_percent").compareTo(BigDecimal.ZERO) >= 0
                                ? "상승 흐름이 강한 종목"
                                : "변동성이 커서 점검이 필요한 종목"
                ),
                staleThreshold()
        );
    }

    private List<HomeScreenResponse.Highlight> buildHighlights(
            HomeScreenResponse.AssetSummary assetSummary,
            HomeScreenResponse.MarketConnection marketConnection,
            ResearchRunSummaryResponse latestRun
    ) {
        HomeScreenResponse.Highlight portfolioHighlight = new HomeScreenResponse.Highlight(
                "내 자산",
                "총 자산 " + money(assetSummary.totalAssets()) + " / 손익 " + signedMoney(assetSummary.totalPnl()),
                assetSummary.totalPnl().compareTo(BigDecimal.ZERO) >= 0 ? "positive" : "negative"
        );
        HomeScreenResponse.Highlight marketHighlight = new HomeScreenResponse.Highlight(
                "시장 연결 상태",
                marketConnection.status() + " / stale quote " + marketConnection.staleQuoteCount() + "건",
                marketConnection.stale() ? "warning" : "positive"
        );
        HomeScreenResponse.Highlight quantHighlight = new HomeScreenResponse.Highlight(
                "오늘의 퀀트 인사이트",
                latestRun != null
                        ? latestRun.strategyName() + "가 " + latestRun.instrumentSymbol() + "을 추적 중입니다."
                        : "아직 연결된 퀀트 인사이트가 없습니다.",
                latestRun != null ? "accent" : "neutral"
        );
        return List.of(portfolioHighlight, marketHighlight, quantHighlight);
    }

    private HomeScreenResponse.QuantSpotlight toQuantSpotlight(ResearchRunSummaryResponse latestRun) {
        ResearchRunDetailResponse detail = researchArtifactService.getRun(latestRun.runId());
        SignalMetadata signal = toSignalMetadata(detail);
        return new HomeScreenResponse.QuantSpotlight(
                latestRun.runId(),
                latestRun.strategyName(),
                latestRun.instrumentSymbol(),
                latestRun.generatedAt(),
                latestRun.metrics(),
                signal.headline(),
                signal.strength()
        );
    }

    private StockDetailResponse.QuantInsight buildQuantInsight(String symbol, ResearchRunDetailResponse detail) {
        if (detail == null) {
            return new StockDetailResponse.QuantInsight(
                    "연결된 전략 없음",
                    "아직 퀀트 인사이트가 없습니다.",
                    symbol + "에 연결된 최신 리서치 artifact가 없어 기본 시세 기반으로만 주문합니다.",
                    "중립",
                    "변동성 데이터 없음",
                    0,
                    Map.of(),
                    List.of("백테스트 run을 생성하면 시그널 설명이 여기에 표시됩니다.")
            );
        }

        SignalMetadata signal = toSignalMetadata(detail);
        return new StockDetailResponse.QuantInsight(
                detail.strategyName(),
                signal.headline(),
                signal.summary(),
                signal.trendLabel(),
                signal.volatilityLabel(),
                signal.strength(),
                detail.metrics(),
                signal.reasons()
        );
    }

    private StockDetailResponse.RiskSummary buildRiskSummary(StockDetailResponse.StockHeader stock) {
        BigDecimal availableCash = jdbcTemplate.query(
                """
                SELECT available_cash
                FROM cash_balance
                ORDER BY account_id
                LIMIT 1
                """,
                (rs, rowNum) -> rs.getBigDecimal("available_cash")
        ).stream().findFirst().orElse(BigDecimal.ZERO.setScale(6, RoundingMode.HALF_UP));

        BigDecimal askPrice = defaultDecimal(stock.askPrice());
        BigDecimal maxAffordable = askPrice.compareTo(BigDecimal.ZERO) > 0
                ? availableCash.divide(askPrice, 4, RoundingMode.DOWN)
                : BigDecimal.ZERO.setScale(4, RoundingMode.DOWN);

        return new StockDetailResponse.RiskSummary(
                availableCash,
                defaultDecimal(stock.askPrice()),
                defaultDecimal(stock.bidPrice()),
                maxAffordable,
                stock.stale(),
                stock.stale() ? "최근 quote가 오래되어 체결 가격 오차가 커질 수 있습니다." : "실시간 quote 기준으로 주문 전 점검을 통과할 수 있습니다.",
                "시장가 매수는 ask, 시장가 매도는 bid 기준으로 예상 체결됩니다."
        );
    }

    private StockDetailResponse.TradeContext loadTradeContext(Long instrumentId) {
        return jdbcTemplate.query(
                """
                SELECT a.id AS account_id,
                       a.account_code,
                       s.id AS strategy_run_id,
                       s.strategy_name,
                       cb.available_cash
                FROM strategy_run s
                JOIN account a ON a.id = s.account_id
                JOIN cash_balance cb ON cb.account_id = a.id
                ORDER BY s.run_at DESC, s.id DESC
                LIMIT 1
                """,
                (rs, rowNum) -> new StockDetailResponse.TradeContext(
                        rs.getLong("account_id"),
                        rs.getString("account_code"),
                        rs.getLong("strategy_run_id"),
                        rs.getString("strategy_name"),
                        rs.getBigDecimal("available_cash")
                )
        ).stream().findFirst().orElse(null);
    }

    private List<StockDetailResponse.PricePoint> loadPriceSeries(Long instrumentId) {
        return jdbcTemplate.query(
                """
                SELECT price_date, close_price
                FROM market_price
                WHERE instrument_id = ?
                ORDER BY price_date DESC, id DESC
                LIMIT 20
                """,
                (rs, rowNum) -> new StockDetailResponse.PricePoint(
                        rs.getDate("price_date").toLocalDate(),
                        rs.getBigDecimal("close_price")
                ),
                instrumentId
        ).stream().sorted(Comparator.comparing(StockDetailResponse.PricePoint::date)).toList();
    }

    private List<StockDetailResponse.ActivityItem> loadRecentOrdersForSymbol(String symbol) {
        return jdbcTemplate.query(
                """
                SELECT o.side,
                       o.status,
                       o.quantity,
                       COALESCE(o.limit_price, mq.last_price, mp.close_price) AS price,
                       o.updated_at
                FROM orders o
                JOIN instrument i ON i.id = o.instrument_id
                LEFT JOIN market_quote mq ON mq.instrument_id = i.id
                LEFT JOIN LATERAL (
                    SELECT close_price
                    FROM market_price mpx
                    WHERE mpx.instrument_id = i.id
                    ORDER BY mpx.price_date DESC, mpx.id DESC
                    LIMIT 1
                ) mp ON true
                WHERE UPPER(i.symbol) = UPPER(?)
                ORDER BY o.updated_at DESC, o.id DESC
                LIMIT 6
                """,
                (rs, rowNum) -> new StockDetailResponse.ActivityItem(
                        "ORDER",
                        rs.getString("side") + " 주문",
                        rs.getString("status"),
                        rs.getBigDecimal("quantity"),
                        rs.getBigDecimal("price"),
                        rs.getTimestamp("updated_at").toLocalDateTime()
                ),
                symbol
        );
    }

    private List<StockDetailResponse.ActivityItem> loadRecentExecutionsForSymbol(String symbol) {
        return jdbcTemplate.query(
                """
                SELECT f.fill_quantity,
                       f.fill_price,
                       f.filled_at
                FROM fill f
                JOIN instrument i ON i.id = f.instrument_id
                WHERE UPPER(i.symbol) = UPPER(?)
                ORDER BY f.filled_at DESC, f.id DESC
                LIMIT 6
                """,
                (rs, rowNum) -> new StockDetailResponse.ActivityItem(
                        "FILL",
                        "체결",
                        "FILLED",
                        rs.getBigDecimal("fill_quantity"),
                        rs.getBigDecimal("fill_price"),
                        rs.getTimestamp("filled_at").toLocalDateTime()
                ),
                symbol
        );
    }

    private QuantOverviewResponse.FeaturedInsight toFeaturedInsight(ResearchRunSummaryResponse latest) {
        ResearchRunDetailResponse detail = researchArtifactService.getRun(latest.runId());
        SignalMetadata signal = toSignalMetadata(detail);
        return new QuantOverviewResponse.FeaturedInsight(
                latest.runId(),
                latest.strategyName(),
                latest.instrumentSymbol(),
                latest.generatedAt(),
                signal.headline(),
                signal.strength(),
                latest.metrics()
        );
    }

    private QuantStrategyDetailResponse.LinkedInstrument loadLinkedInstrument(String symbol) {
        return jdbcTemplate.query(
                """
                SELECT i.symbol,
                       i.name,
                       i.market,
                       COALESCE(mq.last_price, mp.close_price) AS last_price,
                       COALESCE(mq.change_percent, 0) AS change_percent,
                       mq.received_at < ? AS stale
                FROM instrument i
                LEFT JOIN market_quote mq ON mq.instrument_id = i.id
                LEFT JOIN LATERAL (
                    SELECT close_price
                    FROM market_price mp
                    WHERE mp.instrument_id = i.id
                    ORDER BY mp.price_date DESC, mp.id DESC
                    LIMIT 1
                ) mp ON true
                WHERE UPPER(i.symbol) = UPPER(?)
                """,
                (rs, rowNum) -> new QuantStrategyDetailResponse.LinkedInstrument(
                        rs.getString("symbol"),
                        rs.getString("name"),
                        rs.getString("market"),
                        rs.getBigDecimal("last_price"),
                        rs.getBigDecimal("change_percent"),
                        rs.getBoolean("stale")
                ),
                staleThreshold(),
                symbol
        ).stream().findFirst().orElse(null);
    }

    private List<QuantStrategyDetailResponse.ActivityItem> loadRecentOrderFeed(String symbol) {
        return jdbcTemplate.query(
                """
                SELECT o.id,
                       o.status,
                       o.side,
                       o.quantity,
                       o.limit_price,
                       o.updated_at
                FROM orders o
                JOIN instrument i ON i.id = o.instrument_id
                WHERE UPPER(i.symbol) = UPPER(?)
                ORDER BY o.updated_at DESC, o.id DESC
                LIMIT 8
                """,
                (rs, rowNum) -> new QuantStrategyDetailResponse.ActivityItem(
                        rs.getLong("id"),
                        rs.getString("status"),
                        rs.getString("side"),
                        rs.getBigDecimal("quantity"),
                        rs.getBigDecimal("limit_price"),
                        rs.getTimestamp("updated_at").toLocalDateTime()
                ),
                symbol
        );
    }

    private MarketDataHealthResponse marketHealth() {
        return marketDataStatusService.health(
                marketDataProperties.isEnabled(),
                marketDataProperties.getApiKey() != null && !marketDataProperties.getApiKey().isBlank()
        );
    }

    private ResearchRunSummaryResponse findLatestRunBySymbol(String symbol) {
        return researchArtifactService.listRuns().stream()
                .filter(run -> run.instrumentSymbol() != null && run.instrumentSymbol().equalsIgnoreCase(symbol))
                .findFirst()
                .orElse(null);
    }

    private SignalMetadata toSignalMetadata(ResearchRunDetailResponse detail) {
        Map<String, Object> latestSignal = detail.signalRows().isEmpty()
                ? Map.of()
                : detail.signalRows().get(detail.signalRows().size() - 1);
        double rawSignal = number(latestSignal.get("raw_signal"));
        double targetExposure = number(latestSignal.get("target_exposure"));
        double position = number(latestSignal.get("position"));
        double rollingVol = number(latestSignal.get("rolling_vol"));
        String headline = rawSignal > 0 ? "상승 추세 신호" : rawSignal < 0 ? "하락 추세 경계" : "중립 신호";
        String summary = "목표 익스포저 " + percent(targetExposure) + ", 현재 포지션 " + percent(position) + " 수준으로 조절합니다.";
        String trendLabel = rawSignal > 0 ? "상방 모멘텀 우위" : rawSignal < 0 ? "하방 모멘텀 우위" : "모멘텀 중립";
        String volatilityLabel = rollingVol > 0.18 ? "변동성 높음" : rollingVol > 0.10 ? "변동성 보통" : "변동성 낮음";
        int strength = (int) Math.round(Math.min(100.0, Math.abs(position) * 100.0));
        List<String> reasons = List.of(
                "빠른 이동평균과 느린 이동평균의 차이로 방향성을 판단합니다.",
                "변동성이 커질수록 목표 비중을 자동으로 낮춥니다.",
                "실시간 quote를 사용해 주문 전 점검과 예상 체결 가격을 안내합니다."
        );
        return new SignalMetadata(headline, summary, trendLabel, volatilityLabel, strength, reasons);
    }

    private BigDecimal latestPriceForSymbol(String symbol) {
        return jdbcTemplate.query(
                """
                SELECT COALESCE(mq.last_price, mp.close_price) AS last_price
                FROM instrument i
                LEFT JOIN market_quote mq ON mq.instrument_id = i.id
                LEFT JOIN LATERAL (
                    SELECT close_price
                    FROM market_price mp
                    WHERE mp.instrument_id = i.id
                    ORDER BY mp.price_date DESC, mp.id DESC
                    LIMIT 1
                ) mp ON true
                WHERE UPPER(i.symbol) = UPPER(?)
                """,
                (rs, rowNum) -> rs.getBigDecimal("last_price"),
                symbol
        ).stream().findFirst().orElse(null);
    }

    private BigDecimal latestChangeForSymbol(String symbol) {
        return jdbcTemplate.query(
                """
                SELECT mq.change_percent
                FROM instrument i
                JOIN market_quote mq ON mq.instrument_id = i.id
                WHERE UPPER(i.symbol) = UPPER(?)
                """,
                (rs, rowNum) -> rs.getBigDecimal("change_percent"),
                symbol
        ).stream().findFirst().orElse(BigDecimal.ZERO.setScale(6, RoundingMode.HALF_UP));
    }

    private Timestamp staleThreshold() {
        return Timestamp.valueOf(LocalDateTime.now().minusSeconds(Math.max(1L, marketDataProperties.getStaleThresholdSeconds())));
    }

    private DiscoverScreenResponse.StockCard mapStockCard(ResultSet rs) throws SQLException {
        return new DiscoverScreenResponse.StockCard(
                rs.getLong("id"),
                rs.getString("symbol"),
                rs.getString("name"),
                rs.getString("market"),
                rs.getBigDecimal("last_price"),
                rs.getBigDecimal("bid_price"),
                rs.getBigDecimal("ask_price"),
                rs.getBigDecimal("change_percent"),
                rs.getBoolean("stale")
        );
    }

    private RowMapper<DiscoverScreenResponse.StockCard> stockCardMapper() {
        return (rs, rowNum) -> mapStockCard(rs);
    }

    private static LocalDateTime toDateTime(Timestamp timestamp) {
        return timestamp != null ? timestamp.toLocalDateTime() : null;
    }

    private static BigDecimal defaultDecimal(BigDecimal value) {
        return value != null ? value : BigDecimal.ZERO.setScale(6, RoundingMode.HALF_UP);
    }

    private static double number(Object value) {
        if (value instanceof Number number) {
            return number.doubleValue();
        }
        if (value instanceof String string && !string.isBlank()) {
            try {
                return Double.parseDouble(string);
            } catch (NumberFormatException ignored) {
                return 0.0;
            }
        }
        return 0.0;
    }

    private static String money(BigDecimal value) {
        return defaultDecimal(value).setScale(2, RoundingMode.HALF_UP).toPlainString();
    }

    private static String signedMoney(BigDecimal value) {
        BigDecimal normalized = defaultDecimal(value).setScale(2, RoundingMode.HALF_UP);
        return normalized.compareTo(BigDecimal.ZERO) > 0 ? "+" + normalized.toPlainString() : normalized.toPlainString();
    }

    private static String percent(double value) {
        return BigDecimal.valueOf(value * 100.0).setScale(0, RoundingMode.HALF_UP).toPlainString() + "%";
    }

    private record SignalMetadata(
            String headline,
            String summary,
            String trendLabel,
            String volatilityLabel,
            int strength,
            List<String> reasons
    ) {
    }
}
