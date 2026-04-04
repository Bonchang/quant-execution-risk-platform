package com.bonchang.qerp.dashboard;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

public record DashboardOverviewResponse(
        Summary summary,
        PortfolioSummary portfolioSummary,
        ResearchSummary researchSummary,
        QuoteSummary quoteSummary,
        MarketDataHealthItem marketDataHealth,
        Map<String, Long> statusCounts,
        List<AccountSummaryItem> accountSummaries,
        List<RecentOrderItem> recentOrders,
        List<RiskCheckItem> recentRiskChecks,
        List<FillItem> recentFills,
        List<PositionItem> positions,
        List<PortfolioSnapshotItem> recentPortfolioSnapshots,
        List<RecentQuoteItem> recentQuotes,
        List<OutboxEventItem> recentOutboxEvents
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
            Long accountId,
            String accountCode,
            String clientOrderId,
            String status,
            String side,
            BigDecimal requestedQuantity,
            BigDecimal limitPrice,
            BigDecimal reservedCashAmount,
            BigDecimal filledQuantity,
            BigDecimal remainingQuantity,
            String orderType,
            String timeInForce,
            String instrumentSymbol,
            String strategyName,
            LocalDateTime createdAt,
            LocalDateTime expiresAt,
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

    public record PortfolioSummary(
            Long strategyRunId,
            Long accountId,
            String accountCode,
            String strategyName,
            LocalDateTime snapshotAt,
            BigDecimal totalMarketValue,
            BigDecimal unrealizedPnl,
            BigDecimal realizedPnl,
            BigDecimal totalPnl,
            BigDecimal returnRate
    ) {
    }

    public record PortfolioSnapshotItem(
            Long id,
            Long strategyRunId,
            Long accountId,
            String accountCode,
            String strategyName,
            LocalDateTime snapshotAt,
            BigDecimal totalMarketValue,
            BigDecimal unrealizedPnl,
            BigDecimal realizedPnl,
            BigDecimal totalPnl,
            BigDecimal returnRate
    ) {
    }

    public record AccountSummaryItem(
            Long accountId,
            String accountCode,
            String ownerName,
            String baseCurrency,
            BigDecimal availableCash,
            BigDecimal reservedCash,
            LocalDateTime updatedAt
    ) {
    }

    public record ResearchSummary(
            String runId,
            String strategyName,
            String instrumentSymbol,
            String generatedAt,
            Map<String, Object> metrics
    ) {
    }

    public record QuoteSummary(
            long totalQuotes,
            long staleQuotes,
            LocalDateTime lastQuoteReceivedAt,
            String source
    ) {
    }

    public record MarketDataHealthItem(
            String status,
            boolean enabled,
            boolean apiKeyConfigured,
            String source,
            LocalDateTime lastRunAt,
            String lastRunStatus,
            LocalDateTime lastQuoteReceivedAt,
            long staleQuoteCount
    ) {
    }

    public record RecentQuoteItem(
            Long instrumentId,
            String symbol,
            String market,
            BigDecimal lastPrice,
            BigDecimal bidPrice,
            BigDecimal askPrice,
            BigDecimal changePercent,
            String source,
            LocalDateTime quoteTime,
            LocalDateTime receivedAt,
            boolean stale
    ) {
    }

    public record OutboxEventItem(
            Long id,
            String aggregateType,
            Long aggregateId,
            String eventType,
            String processingStatus,
            LocalDateTime createdAt,
            LocalDateTime processedAt
    ) {
    }
}
