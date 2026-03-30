package com.bonchang.qerp.dashboard;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class DashboardService {

    private final JdbcTemplate jdbcTemplate;

    public DashboardOverviewResponse loadOverview(int limit) {
        Map<String, Long> statusCounts = loadStatusCounts();
        List<DashboardOverviewResponse.RecentOrderItem> recentOrders = loadRecentOrders(limit);
        List<DashboardOverviewResponse.RiskCheckItem> recentRiskChecks = loadRecentRiskChecks(limit);
        List<DashboardOverviewResponse.FillItem> recentFills = loadRecentFills(limit);
        List<DashboardOverviewResponse.PositionItem> positions = loadPositions(limit);

        return new DashboardOverviewResponse(statusCounts, recentOrders, recentRiskChecks, recentFills, positions);
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

    private List<DashboardOverviewResponse.RecentOrderItem> loadRecentOrders(int limit) {
        return jdbcTemplate.query(
                """
                SELECT o.id,
                       o.client_order_id,
                       o.status,
                       o.side,
                       o.quantity,
                       o.order_type,
                       i.symbol AS instrument_symbol,
                       s.strategy_name,
                       o.created_at
                FROM orders o
                JOIN instrument i ON i.id = o.instrument_id
                JOIN strategy_run s ON s.id = o.strategy_run_id
                ORDER BY o.id DESC
                LIMIT ?
                """,
                (rs, rowNum) -> new DashboardOverviewResponse.RecentOrderItem(
                        rs.getLong("id"),
                        rs.getString("client_order_id"),
                        rs.getString("status"),
                        rs.getString("side"),
                        rs.getBigDecimal("quantity"),
                        rs.getString("order_type"),
                        rs.getString("instrument_symbol"),
                        rs.getString("strategy_name"),
                        rs.getTimestamp("created_at").toLocalDateTime()
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
}
