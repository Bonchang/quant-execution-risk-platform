package com.bonchang.qerp.marketdata;

import java.time.LocalDateTime;
import java.util.List;

public record MarketDataHealthResponse(
        String status,
        boolean enabled,
        boolean apiKeyConfigured,
        String source,
        LocalDateTime lastRunAt,
        String lastRunStatus,
        LocalDateTime lastQuoteReceivedAt,
        long totalQuotes,
        long staleQuoteCount,
        List<String> staleSymbols,
        List<String> recentFailures
) {
}
