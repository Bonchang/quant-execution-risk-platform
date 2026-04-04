package com.bonchang.qerp.appview;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

public record QuantOverviewResponse(
        FeaturedInsight featuredInsight,
        List<StrategyCard> strategies
) {

    public record FeaturedInsight(
            String runId,
            String strategyName,
            String instrumentSymbol,
            String generatedAt,
            String headline,
            int signalStrength,
            Map<String, Object> metrics
    ) {
    }

    public record StrategyCard(
            String runId,
            String strategyName,
            String instrumentSymbol,
            String generatedAt,
            BigDecimal lastPrice,
            BigDecimal changePercent,
            Map<String, Object> metrics
    ) {
    }
}
