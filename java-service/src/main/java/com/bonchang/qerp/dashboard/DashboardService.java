package com.bonchang.qerp.dashboard;

import com.bonchang.qerp.account.Account;
import com.bonchang.qerp.account.AccountService;
import com.bonchang.qerp.marketdata.MarketDataHealthResponse;
import com.bonchang.qerp.marketdata.MarketDataProperties;
import com.bonchang.qerp.marketdata.MarketDataStatusService;
import com.bonchang.qerp.portfolio.PortfolioSnapshotService;
import com.bonchang.qerp.research.ResearchArtifactService;
import com.bonchang.qerp.research.ResearchRunSummaryResponse;
import java.math.BigDecimal;
import java.sql.Date;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class DashboardService {

    private static final int DEFAULT_OPTION_LIMIT = 100;

    private final JdbcTemplate jdbcTemplate;
    private final PortfolioSnapshotService portfolioSnapshotService;
    private final AccountService accountService;
    private final ResearchArtifactService researchArtifactService;
    private final MarketDataStatusService marketDataStatusService;
    private final MarketDataProperties marketDataProperties;

    public DashboardOverviewResponse loadOverview(int limit) {
        Map<String, Long> statusCounts = loadStatusCounts();
        DashboardOverviewResponse.Summary summary = buildSummary(statusCounts);
        DashboardOverviewResponse.PortfolioSummary portfolioSummary = loadLatestPortfolioSummary();
        DashboardOverviewResponse.ResearchSummary researchSummary = loadLatestResearchSummary();
        DashboardOverviewResponse.QuoteSummary quoteSummary = loadQuoteSummary();
        DashboardOverviewResponse.MarketDataHealthItem marketDataHealth = loadMarketDataHealth();
        List<DashboardOverviewResponse.AccountSummaryItem> accountSummaries = loadAccountSummaries();
        List<DashboardOverviewResponse.RecentOrderItem> recentOrders = loadRecentOrders(limit);
        List<DashboardOverviewResponse.RiskCheckItem> recentRiskChecks = loadRecentRiskChecks(limit);
        List<DashboardOverviewResponse.FillItem> recentFills = loadRecentFills(limit);
        List<DashboardOverviewResponse.PositionItem> positions = loadPositions(limit);
        List<DashboardOverviewResponse.PortfolioSnapshotItem> recentPortfolioSnapshots = loadRecentPortfolioSnapshots(limit);
        List<DashboardOverviewResponse.RecentQuoteItem> recentQuotes = loadRecentQuotes(limit);
        List<DashboardOverviewResponse.OutboxEventItem> recentOutboxEvents = loadRecentOutboxEvents(limit);

        return new DashboardOverviewResponse(
                summary,
                portfolioSummary,
                researchSummary,
                quoteSummary,
                marketDataHealth,
                statusCounts,
                accountSummaries,
                recentOrders,
                recentRiskChecks,
                recentFills,
                positions,
                recentPortfolioSnapshots,
                recentQuotes,
                recentOutboxEvents
        );
    }

    public DashboardTimelineResponse loadTimeline(int limit) {
        return new DashboardTimelineResponse(
                jdbcTemplate.query(
                        """
                        SELECT category,
                               event_type,
                               severity,
                               title,
                               description,
                               subject_key,
                               occurred_at
                        FROM (
                            SELECT 'ORDER' AS category,
                                   CAST(o.status AS VARCHAR(64)) AS event_type,
                                   CASE WHEN o.status = 'REJECTED' THEN 'ERROR' WHEN o.status = 'WORKING' THEN 'WARN' ELSE 'INFO' END AS severity,
                                   'Order #' || o.id || ' ' || o.status AS title,
                                   i.symbol || ' ' || CAST(o.side AS VARCHAR(16)) || ' ' || CAST(o.quantity AS VARCHAR(32)) AS description,
                                   'order:' || o.id AS subject_key,
                                   o.updated_at AS occurred_at
                            FROM orders o
                            JOIN instrument i ON i.id = o.instrument_id
                            UNION ALL
                            SELECT 'RISK',
                                   r.rule_name,
                                   CASE WHEN r.passed THEN 'INFO' ELSE 'ERROR' END,
                                   'Risk check ' || r.rule_name,
                                   r.message,
                                   'order:' || r.order_id,
                                   r.checked_at
                            FROM risk_check_result r
                            UNION ALL
                            SELECT 'FILL',
                                   'FILL',
                                   'INFO',
                                   'Fill #' || f.id,
                                   i.symbol || ' @ ' || CAST(f.fill_price AS VARCHAR(32)),
                                   'order:' || f.order_id,
                                   f.filled_at
                            FROM fill f
                            JOIN instrument i ON i.id = f.instrument_id
                            UNION ALL
                            SELECT 'OUTBOX',
                                   e.event_type,
                                   CASE WHEN e.processing_status = 'FAILED' THEN 'ERROR' ELSE 'INFO' END,
                                   e.aggregate_type || ' event',
                                   e.processing_status,
                                   lower(e.aggregate_type) || ':' || e.aggregate_id,
                                   e.created_at
                            FROM outbox_event e
                            UNION ALL
                            SELECT 'QUOTE',
                                   CASE WHEN mq.received_at < ? THEN 'QUOTE_STALE' ELSE 'QUOTE_UPDATED' END,
                                   CASE WHEN mq.received_at < ? THEN 'WARN' ELSE 'INFO' END,
                                   'Quote ' || i.symbol,
                                   'last=' || CAST(mq.last_price AS VARCHAR(32)) || ', bid=' || CAST(mq.bid_price AS VARCHAR(32)) || ', ask=' || CAST(mq.ask_price AS VARCHAR(32)),
                                   'instrument:' || mq.instrument_id,
                                   mq.received_at
                            FROM market_quote mq
                            JOIN instrument i ON i.id = mq.instrument_id
                        ) timeline
                        ORDER BY occurred_at DESC
                        LIMIT ?
                        """,
                        (rs, rowNum) -> new DashboardTimelineResponse.TimelineEventItem(
                                rs.getString("category"),
                                rs.getString("event_type"),
                                rs.getString("severity"),
                                rs.getString("title"),
                                rs.getString("description"),
                                rs.getString("subject_key"),
                                rs.getTimestamp("occurred_at").toLocalDateTime()
                        ),
                        java.sql.Timestamp.valueOf(LocalDateTime.now().minusSeconds(Math.max(1L, marketDataProperties.getStaleThresholdSeconds()))),
                        java.sql.Timestamp.valueOf(LocalDateTime.now().minusSeconds(Math.max(1L, marketDataProperties.getStaleThresholdSeconds()))),
                        limit
                )
        );
    }

    public DashboardOptionsResponse loadOptions() {
        List<DashboardOptionsResponse.AccountOption> accounts = jdbcTemplate.query(
                """
                SELECT id, account_code, owner_name, base_currency
                FROM account
                ORDER BY account_code
                LIMIT ?
                """,
                (rs, rowNum) -> new DashboardOptionsResponse.AccountOption(
                        rs.getLong("id"),
                        rs.getString("account_code"),
                        rs.getString("owner_name"),
                        rs.getString("base_currency")
                ),
                DEFAULT_OPTION_LIMIT
        );

        List<DashboardOptionsResponse.StrategyRunOption> strategyRuns = jdbcTemplate.query(
                """
                SELECT id, account_id, strategy_name, run_at
                FROM strategy_run
                ORDER BY run_at DESC, id DESC
                LIMIT ?
                """,
                (rs, rowNum) -> new DashboardOptionsResponse.StrategyRunOption(
                        rs.getLong("id"),
                        rs.getLong("account_id"),
                        rs.getString("strategy_name"),
                        rs.getTimestamp("run_at").toLocalDateTime().toString()
                ),
                DEFAULT_OPTION_LIMIT
        );

        List<DashboardOptionsResponse.InstrumentOption> instruments = jdbcTemplate.query(
                """
                SELECT i.id,
                       i.symbol,
                       i.name,
                       i.market,
                       lp.close_price,
                       lp.price_date
                FROM instrument i
                LEFT JOIN LATERAL (
                    SELECT mp.close_price, mp.price_date
                    FROM market_price mp
                    WHERE mp.instrument_id = i.id
                    ORDER BY mp.price_date DESC, mp.id DESC
                    LIMIT 1
                ) lp ON true
                ORDER BY i.symbol
                LIMIT ?
                """,
                (rs, rowNum) -> {
                    Date priceDate = rs.getDate("price_date");
                    return new DashboardOptionsResponse.InstrumentOption(
                            rs.getLong("id"),
                            rs.getString("symbol"),
                            rs.getString("name"),
                            rs.getString("market"),
                            rs.getBigDecimal("close_price"),
                            priceDate != null ? priceDate.toLocalDate() : null
                    );
                },
                DEFAULT_OPTION_LIMIT
        );

        return new DashboardOptionsResponse(accounts, strategyRuns, instruments);
    }

    @Transactional
    public DashboardSeedResponse seedDemoData() {
        Account account = accountService.ensureDemoAccount("DEMO-001", "Portfolio Demo Account", new BigDecimal("250000.000000"));

        Long instrumentId = jdbcTemplate.queryForObject(
                """
                INSERT INTO instrument(symbol, name, market)
                VALUES ('DEMO_AAPL', 'Apple Inc. (Demo)', 'NASDAQ')
                ON CONFLICT (symbol) DO UPDATE
                    SET name = EXCLUDED.name,
                        market = EXCLUDED.market
                RETURNING id
                """,
                Long.class
        );

        LocalDate today = LocalDate.now();
        jdbcTemplate.update(
                """
                INSERT INTO market_price(instrument_id, price_date, open_price, high_price, low_price, close_price, volume)
                SELECT ?, ?, 185.000000, 189.000000, 183.000000, 187.500000, 1500000
                WHERE NOT EXISTS (
                    SELECT 1 FROM market_price WHERE instrument_id = ? AND price_date = ?
                )
                """,
                instrumentId,
                Date.valueOf(today),
                instrumentId,
                Date.valueOf(today)
        );

        jdbcTemplate.update(
                """
                INSERT INTO market_quote(instrument_id, quote_time, last_price, bid_price, ask_price, change_percent, source, received_at)
                VALUES (?, NOW(), 187.500000, 187.406250, 187.593750, 0.850000, 'DASHBOARD_SEED', NOW())
                ON CONFLICT (instrument_id) DO UPDATE
                    SET quote_time = EXCLUDED.quote_time,
                        last_price = EXCLUDED.last_price,
                        bid_price = EXCLUDED.bid_price,
                        ask_price = EXCLUDED.ask_price,
                        change_percent = EXCLUDED.change_percent,
                        source = EXCLUDED.source,
                        received_at = EXCLUDED.received_at
                """,
                instrumentId
        );

        Long strategyRunId = jdbcTemplate.queryForObject(
                """
                INSERT INTO strategy_run(account_id, strategy_name, run_at, parameters_json)
                VALUES (?, 'demo-strategy', ?, '{"source":"dashboard-seed","strategy":"demo"}')
                RETURNING id
                """,
                Long.class,
                account.getId(),
                java.sql.Timestamp.valueOf(LocalDateTime.now())
        );

        return new DashboardSeedResponse(
                account.getId(),
                account.getAccountCode(),
                strategyRunId,
                instrumentId,
                "DEMO_AAPL",
                "Demo account, instrument, market price, and strategy run created"
        );
    }

    @Transactional
    public DashboardPortfolioRefreshResponse refreshPortfolioSnapshots() {
        int count = portfolioSnapshotService.refreshSnapshotsForAllStrategyRuns();
        return new DashboardPortfolioRefreshResponse(
                count,
                "Portfolio snapshots refreshed for " + count + " strategy runs"
        );
    }

    private Map<String, Long> loadStatusCounts() {
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                "SELECT status, COUNT(*) AS cnt FROM orders GROUP BY status ORDER BY status"
        );
        Map<String, Long> counts = new LinkedHashMap<>();
        for (Map<String, Object> row : rows) {
            counts.put((String) row.get("status"), ((Number) row.get("cnt")).longValue());
        }
        return counts;
    }

    private DashboardOverviewResponse.Summary buildSummary(Map<String, Long> statusCounts) {
        long totalOrders = statusCounts.values().stream().mapToLong(Long::longValue).sum();
        long filledOrders = statusCounts.getOrDefault("FILLED", 0L);
        long rejectedOrders = statusCounts.getOrDefault("REJECTED", 0L);

        double fillRatePercent = totalOrders > 0
                ? (filledOrders * 100.0) / totalOrders
                : 0.0;
        double rejectionRatePercent = totalOrders > 0
                ? (rejectedOrders * 100.0) / totalOrders
                : 0.0;

        return new DashboardOverviewResponse.Summary(
                totalOrders,
                filledOrders,
                rejectedOrders,
                fillRatePercent,
                rejectionRatePercent
        );
    }

    private List<DashboardOverviewResponse.AccountSummaryItem> loadAccountSummaries() {
        return jdbcTemplate.query(
                """
                SELECT a.id AS account_id,
                       a.account_code,
                       a.owner_name,
                       a.base_currency,
                       cb.available_cash,
                       cb.reserved_cash,
                       cb.updated_at
                FROM account a
                LEFT JOIN cash_balance cb ON cb.account_id = a.id
                ORDER BY a.account_code
                """,
                (rs, rowNum) -> new DashboardOverviewResponse.AccountSummaryItem(
                        rs.getLong("account_id"),
                        rs.getString("account_code"),
                        rs.getString("owner_name"),
                        rs.getString("base_currency"),
                        rs.getBigDecimal("available_cash"),
                        rs.getBigDecimal("reserved_cash"),
                        rs.getTimestamp("updated_at") != null ? rs.getTimestamp("updated_at").toLocalDateTime() : null
                )
        );
    }

    private List<DashboardOverviewResponse.RecentOrderItem> loadRecentOrders(int limit) {
        return jdbcTemplate.query(
                """
                SELECT o.id,
                       o.account_id,
                       a.account_code,
                       o.client_order_id,
                       o.status,
                       o.side,
                       o.quantity AS requested_quantity,
                       o.limit_price,
                       o.reserved_cash_amount,
                       o.filled_quantity,
                       o.remaining_quantity,
                       o.order_type,
                       o.time_in_force,
                       i.symbol AS instrument_symbol,
                       s.strategy_name,
                       o.created_at,
                       o.expires_at,
                       o.updated_at
                FROM orders o
                JOIN account a ON a.id = o.account_id
                JOIN instrument i ON i.id = o.instrument_id
                JOIN strategy_run s ON s.id = o.strategy_run_id
                ORDER BY o.id DESC
                LIMIT ?
                """,
                (rs, rowNum) -> new DashboardOverviewResponse.RecentOrderItem(
                        rs.getLong("id"),
                        rs.getLong("account_id"),
                        rs.getString("account_code"),
                        rs.getString("client_order_id"),
                        rs.getString("status"),
                        rs.getString("side"),
                        rs.getBigDecimal("requested_quantity"),
                        rs.getBigDecimal("limit_price"),
                        rs.getBigDecimal("reserved_cash_amount"),
                        rs.getBigDecimal("filled_quantity"),
                        rs.getBigDecimal("remaining_quantity"),
                        rs.getString("order_type"),
                        rs.getString("time_in_force"),
                        rs.getString("instrument_symbol"),
                        rs.getString("strategy_name"),
                        rs.getTimestamp("created_at").toLocalDateTime(),
                        rs.getTimestamp("expires_at") != null ? rs.getTimestamp("expires_at").toLocalDateTime() : null,
                        rs.getTimestamp("updated_at").toLocalDateTime()
                ),
                limit
        );
    }

    private List<DashboardOverviewResponse.RiskCheckItem> loadRecentRiskChecks(int limit) {
        return jdbcTemplate.query(
                """
                SELECT id, order_id, rule_name, passed, message, checked_at
                FROM risk_check_result
                ORDER BY id DESC
                LIMIT ?
                """,
                (rs, rowNum) -> new DashboardOverviewResponse.RiskCheckItem(
                        rs.getLong("id"),
                        rs.getLong("order_id"),
                        rs.getString("rule_name"),
                        rs.getBoolean("passed"),
                        rs.getString("message"),
                        rs.getTimestamp("checked_at").toLocalDateTime()
                ),
                limit
        );
    }

    private List<DashboardOverviewResponse.FillItem> loadRecentFills(int limit) {
        return jdbcTemplate.query(
                """
                SELECT f.id,
                       f.order_id,
                       i.symbol AS instrument_symbol,
                       f.fill_quantity,
                       f.fill_price,
                       f.filled_at
                FROM fill f
                JOIN instrument i ON i.id = f.instrument_id
                ORDER BY f.id DESC
                LIMIT ?
                """,
                (rs, rowNum) -> new DashboardOverviewResponse.FillItem(
                        rs.getLong("id"),
                        rs.getLong("order_id"),
                        rs.getString("instrument_symbol"),
                        rs.getBigDecimal("fill_quantity"),
                        rs.getBigDecimal("fill_price"),
                        rs.getTimestamp("filled_at").toLocalDateTime()
                ),
                limit
        );
    }

    private List<DashboardOverviewResponse.PositionItem> loadPositions(int limit) {
        return jdbcTemplate.query(
                """
                SELECT p.id,
                       s.strategy_name,
                       i.symbol AS instrument_symbol,
                       p.net_quantity,
                       p.average_price,
                       p.updated_at
                FROM position p
                JOIN strategy_run s ON s.id = p.strategy_run_id
                JOIN instrument i ON i.id = p.instrument_id
                WHERE p.net_quantity <> 0
                ORDER BY p.updated_at DESC
                LIMIT ?
                """,
                (rs, rowNum) -> new DashboardOverviewResponse.PositionItem(
                        rs.getLong("id"),
                        rs.getString("strategy_name"),
                        rs.getString("instrument_symbol"),
                        rs.getBigDecimal("net_quantity"),
                        rs.getBigDecimal("average_price"),
                        rs.getTimestamp("updated_at").toLocalDateTime()
                ),
                limit
        );
    }

    private DashboardOverviewResponse.PortfolioSummary loadLatestPortfolioSummary() {
        List<DashboardOverviewResponse.PortfolioSummary> rows = jdbcTemplate.query(
                """
                SELECT ps.strategy_run_id,
                       s.account_id,
                       a.account_code,
                       s.strategy_name,
                       ps.snapshot_at,
                       ps.total_market_value,
                       ps.unrealized_pnl,
                       ps.realized_pnl,
                       ps.total_pnl,
                       ps.return_rate
                FROM portfolio_snapshot ps
                JOIN strategy_run s ON s.id = ps.strategy_run_id
                JOIN account a ON a.id = s.account_id
                ORDER BY ps.snapshot_at DESC, ps.id DESC
                LIMIT 1
                """,
                (rs, rowNum) -> new DashboardOverviewResponse.PortfolioSummary(
                        rs.getLong("strategy_run_id"),
                        rs.getLong("account_id"),
                        rs.getString("account_code"),
                        rs.getString("strategy_name"),
                        rs.getTimestamp("snapshot_at").toLocalDateTime(),
                        rs.getBigDecimal("total_market_value"),
                        rs.getBigDecimal("unrealized_pnl"),
                        rs.getBigDecimal("realized_pnl"),
                        rs.getBigDecimal("total_pnl"),
                        rs.getBigDecimal("return_rate")
                )
        );

        if (rows.isEmpty()) {
            return new DashboardOverviewResponse.PortfolioSummary(
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null
            );
        }
        return rows.get(0);
    }

    private List<DashboardOverviewResponse.PortfolioSnapshotItem> loadRecentPortfolioSnapshots(int limit) {
        return jdbcTemplate.query(
                """
                SELECT ps.id,
                       ps.strategy_run_id,
                       s.account_id,
                       a.account_code,
                       s.strategy_name,
                       ps.snapshot_at,
                       ps.total_market_value,
                       ps.unrealized_pnl,
                       ps.realized_pnl,
                       ps.total_pnl,
                       ps.return_rate
                FROM portfolio_snapshot ps
                JOIN strategy_run s ON s.id = ps.strategy_run_id
                JOIN account a ON a.id = s.account_id
                ORDER BY ps.snapshot_at DESC, ps.id DESC
                LIMIT ?
                """,
                (rs, rowNum) -> new DashboardOverviewResponse.PortfolioSnapshotItem(
                        rs.getLong("id"),
                        rs.getLong("strategy_run_id"),
                        rs.getLong("account_id"),
                        rs.getString("account_code"),
                        rs.getString("strategy_name"),
                        rs.getTimestamp("snapshot_at").toLocalDateTime(),
                        rs.getBigDecimal("total_market_value"),
                        rs.getBigDecimal("unrealized_pnl"),
                        rs.getBigDecimal("realized_pnl"),
                        rs.getBigDecimal("total_pnl"),
                        rs.getBigDecimal("return_rate")
                ),
                limit
        );
    }

    private List<DashboardOverviewResponse.OutboxEventItem> loadRecentOutboxEvents(int limit) {
        return jdbcTemplate.query(
                """
                SELECT id, aggregate_type, aggregate_id, event_type, processing_status, created_at, processed_at
                FROM outbox_event
                ORDER BY created_at DESC, id DESC
                LIMIT ?
                """,
                (rs, rowNum) -> new DashboardOverviewResponse.OutboxEventItem(
                        rs.getLong("id"),
                        rs.getString("aggregate_type"),
                        rs.getLong("aggregate_id"),
                        rs.getString("event_type"),
                        rs.getString("processing_status"),
                        rs.getTimestamp("created_at").toLocalDateTime(),
                        rs.getTimestamp("processed_at") != null ? rs.getTimestamp("processed_at").toLocalDateTime() : null
                ),
                limit
        );
    }

    private DashboardOverviewResponse.ResearchSummary loadLatestResearchSummary() {
        ResearchRunSummaryResponse latest = researchArtifactService.latestRun();
        if (latest == null) {
            return new DashboardOverviewResponse.ResearchSummary(null, null, null, null, Map.of());
        }
        return new DashboardOverviewResponse.ResearchSummary(
                latest.runId(),
                latest.strategyName(),
                latest.instrumentSymbol(),
                latest.generatedAt(),
                latest.metrics()
        );
    }

    private DashboardOverviewResponse.QuoteSummary loadQuoteSummary() {
        Map<String, Object> row = jdbcTemplate.queryForMap(
                """
                SELECT COUNT(*) AS total_quotes,
                       COALESCE(SUM(CASE WHEN received_at < ? THEN 1 ELSE 0 END), 0) AS stale_quotes,
                       MAX(received_at) AS last_quote_received_at,
                       MAX(source) AS source
                FROM market_quote
                """,
                java.sql.Timestamp.valueOf(LocalDateTime.now().minusSeconds(Math.max(1L, marketDataProperties.getStaleThresholdSeconds())))
        );
        return new DashboardOverviewResponse.QuoteSummary(
                ((Number) row.get("total_quotes")).longValue(),
                ((Number) row.get("stale_quotes")).longValue(),
                row.get("last_quote_received_at") != null
                        ? ((java.sql.Timestamp) row.get("last_quote_received_at")).toLocalDateTime()
                        : null,
                (String) row.get("source")
        );
    }

    private DashboardOverviewResponse.MarketDataHealthItem loadMarketDataHealth() {
        MarketDataHealthResponse response = marketDataStatusService.health(
                marketDataProperties.isEnabled(),
                marketDataProperties.getApiKey() != null && !marketDataProperties.getApiKey().isBlank()
        );
        return new DashboardOverviewResponse.MarketDataHealthItem(
                response.status(),
                response.enabled(),
                response.apiKeyConfigured(),
                response.source(),
                response.lastRunAt(),
                response.lastRunStatus(),
                response.lastQuoteReceivedAt(),
                response.staleQuoteCount()
        );
    }

    private List<DashboardOverviewResponse.RecentQuoteItem> loadRecentQuotes(int limit) {
        return jdbcTemplate.query(
                """
                SELECT mq.instrument_id,
                       i.symbol,
                       i.market,
                       mq.last_price,
                       mq.bid_price,
                       mq.ask_price,
                       mq.change_percent,
                       mq.source,
                       mq.quote_time,
                       mq.received_at
                FROM market_quote mq
                JOIN instrument i ON i.id = mq.instrument_id
                ORDER BY mq.received_at DESC, mq.id DESC
                LIMIT ?
                """,
                (rs, rowNum) -> new DashboardOverviewResponse.RecentQuoteItem(
                        rs.getLong("instrument_id"),
                        rs.getString("symbol"),
                        rs.getString("market"),
                        rs.getBigDecimal("last_price"),
                        rs.getBigDecimal("bid_price"),
                        rs.getBigDecimal("ask_price"),
                        rs.getBigDecimal("change_percent"),
                        rs.getString("source"),
                        rs.getTimestamp("quote_time").toLocalDateTime(),
                        rs.getTimestamp("received_at").toLocalDateTime(),
                        rs.getTimestamp("received_at").toLocalDateTime()
                                .isBefore(LocalDateTime.now().minusSeconds(Math.max(1L, marketDataProperties.getStaleThresholdSeconds())))
                ),
                limit
        );
    }
}
