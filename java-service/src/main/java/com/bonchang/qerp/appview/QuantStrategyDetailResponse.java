package com.bonchang.qerp.appview;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

public record QuantStrategyDetailResponse(
        String runId,
        String strategyName,
        String instrumentSymbol,
        String generatedAt,
        Map<String, Object> metrics,
        Map<String, Object> config,
        Map<String, Boolean> artifactAvailability,
        List<Map<String, Object>> equityCurveRows,
        List<Map<String, Object>> tradeRows,
        List<Map<String, Object>> signalRows,
        LinkedInstrument linkedInstrument,
        List<ActivityItem> recentOrders
) {

    public record LinkedInstrument(
            String symbol,
            String name,
            String market,
            BigDecimal lastPrice,
            BigDecimal changePercent,
            boolean stale
    ) {
    }

    public record ActivityItem(
            Long orderId,
            String status,
            String side,
            BigDecimal quantity,
            BigDecimal limitPrice,
            LocalDateTime updatedAt
    ) {
    }
}
