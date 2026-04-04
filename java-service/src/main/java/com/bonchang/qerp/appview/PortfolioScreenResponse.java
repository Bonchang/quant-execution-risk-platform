package com.bonchang.qerp.appview;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record PortfolioScreenResponse(
        AssetSummary assetSummary,
        AccountState account,
        List<HoldingItem> holdings,
        List<TrendPoint> assetTrend,
        List<ExecutionItem> recentExecutions
) {

    public record AssetSummary(
            BigDecimal totalAssets,
            BigDecimal cashAmount,
            BigDecimal investedAmount,
            BigDecimal totalPnl,
            BigDecimal returnRate,
            LocalDateTime snapshotAt
    ) {
    }

    public record AccountState(
            Long accountId,
            String accountCode,
            String ownerName,
            String baseCurrency,
            BigDecimal availableCash,
            BigDecimal reservedCash
    ) {
    }

    public record HoldingItem(
            String symbol,
            String strategyName,
            BigDecimal netQuantity,
            BigDecimal averagePrice,
            BigDecimal lastPrice,
            BigDecimal marketValue,
            BigDecimal unrealizedPnl
    ) {
    }

    public record TrendPoint(
            LocalDateTime snapshotAt,
            BigDecimal totalAssets,
            BigDecimal totalPnl
    ) {
    }

    public record ExecutionItem(
            Long orderId,
            String symbol,
            BigDecimal fillQuantity,
            BigDecimal fillPrice,
            LocalDateTime filledAt
    ) {
    }
}
