package com.bonchang.qerp.appview;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

public record StockDetailResponse(
        StockHeader stock,
        List<PricePoint> priceSeries,
        QuantInsight quantInsight,
        RiskSummary riskSummary,
        TradeContext tradeContext,
        List<ActivityItem> recentOrders,
        List<ActivityItem> recentExecutions
) {

    public record StockHeader(
            Long instrumentId,
            String symbol,
            String name,
            String market,
            BigDecimal lastPrice,
            BigDecimal bidPrice,
            BigDecimal askPrice,
            BigDecimal changePercent,
            boolean stale,
            LocalDateTime receivedAt,
            String marketStatus
    ) {
    }

    public record PricePoint(
            LocalDate date,
            BigDecimal closePrice
    ) {
    }

    public record QuantInsight(
            String strategyName,
            String headline,
            String summary,
            String trendLabel,
            String volatilityLabel,
            int signalStrength,
            Map<String, Object> metrics,
            List<String> reasons
    ) {
    }

    public record RiskSummary(
            BigDecimal availableCash,
            BigDecimal estimatedBuyPrice,
            BigDecimal estimatedSellPrice,
            BigDecimal maxAffordableQuantity,
            boolean staleQuote,
            String staleMessage,
            String executionHint
    ) {
    }

    public record TradeContext(
            Long accountId,
            String accountCode,
            Long strategyRunId,
            String strategyName,
            BigDecimal availableCash
    ) {
    }

    public record ActivityItem(
            String type,
            String title,
            String status,
            BigDecimal quantity,
            BigDecimal price,
            LocalDateTime occurredAt
    ) {
    }
}
