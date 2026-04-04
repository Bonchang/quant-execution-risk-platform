package com.bonchang.qerp.marketdata;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record MarketQuoteResponse(
        Long instrumentId,
        String symbol,
        String market,
        LocalDateTime quoteTime,
        BigDecimal lastPrice,
        BigDecimal bidPrice,
        BigDecimal askPrice,
        BigDecimal changePercent,
        String source,
        LocalDateTime receivedAt,
        boolean stale
) {
}
