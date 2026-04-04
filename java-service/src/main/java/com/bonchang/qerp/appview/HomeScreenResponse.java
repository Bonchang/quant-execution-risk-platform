package com.bonchang.qerp.appview;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

public record HomeScreenResponse(
        boolean guestAvailable,
        AssetSummary assetSummary,
        MarketConnection marketConnection,
        List<Highlight> highlights,
        List<FeaturedStock> featuredStocks,
        QuantSpotlight quantSpotlight
) {

    public record AssetSummary(
            Long accountId,
            String accountCode,
            String ownerName,
            String baseCurrency,
            BigDecimal totalAssets,
            BigDecimal investedAmount,
            BigDecimal cashAmount,
            BigDecimal totalPnl,
            BigDecimal returnRate,
            LocalDateTime snapshotAt
    ) {
    }

    public record MarketConnection(
            String status,
            String source,
            boolean stale,
            long staleQuoteCount,
            LocalDateTime lastQuoteReceivedAt
    ) {
    }

    public record Highlight(
            String title,
            String body,
            String tone
    ) {
    }

    public record FeaturedStock(
            String symbol,
            String name,
            String market,
            BigDecimal lastPrice,
            BigDecimal changePercent,
            boolean stale,
            String reason
    ) {
    }

    public record QuantSpotlight(
            String runId,
            String strategyName,
            String instrumentSymbol,
            String generatedAt,
            Map<String, Object> metrics,
            String signalHeadline,
            int signalStrength
    ) {
    }
}
