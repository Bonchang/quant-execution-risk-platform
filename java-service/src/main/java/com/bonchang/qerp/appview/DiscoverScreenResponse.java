package com.bonchang.qerp.appview;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record DiscoverScreenResponse(
        MarketSummary marketSummary,
        List<StockCard> stocks
) {

    public record MarketSummary(
            String marketStatus,
            long liveQuoteCount,
            long staleQuoteCount,
            LocalDateTime lastQuoteReceivedAt,
            String source,
            String topMoverSymbol,
            BigDecimal topMoverChangePercent
    ) {
    }

    public record StockCard(
            Long instrumentId,
            String symbol,
            String name,
            String market,
            BigDecimal lastPrice,
            BigDecimal bidPrice,
            BigDecimal askPrice,
            BigDecimal changePercent,
            boolean stale
    ) {
    }
}
