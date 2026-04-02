package com.bonchang.qerp.dashboard;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

public record DashboardOverviewResponse(
        Summary summary,
        Map<String, Long> statusCounts,
        List<RecentOrderItem> recentOrders,
        List<RiskCheckItem> recentRiskChecks,
        List<FillItem> recentFills,
        List<PositionItem> positions
) {

    public record Summary(
            long totalOrders,
            long filledOrders,
            long rejectedOrders,
            double fillRatePercent,
            double rejectionRatePercent
    ) {
    }

    public record RecentOrderItem(
            Long id,
            String clientOrderId,
            String status,
            String side,
            BigDecimal requestedQuantity,
            BigDecimal limitPrice,
            BigDecimal filledQuantity,
            BigDecimal remainingQuantity,
            String orderType,
            String instrumentSymbol,
            String strategyName,
            LocalDateTime createdAt,
            LocalDateTime updatedAt
    ) {
    }

    public record RiskCheckItem(
            Long id,
            Long orderId,
            String ruleName,
            boolean passed,
            String message,
            LocalDateTime checkedAt
    ) {
    }

    public record FillItem(
            Long id,
            Long orderId,
            String instrumentSymbol,
            BigDecimal fillQuantity,
            BigDecimal fillPrice,
            LocalDateTime filledAt
    ) {
    }

    public record PositionItem(
            Long id,
            String strategyName,
            String instrumentSymbol,
            BigDecimal netQuantity,
            BigDecimal averagePrice,
            LocalDateTime updatedAt
    ) {
    }
}
