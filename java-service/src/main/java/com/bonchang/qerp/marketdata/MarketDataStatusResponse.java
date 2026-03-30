package com.bonchang.qerp.marketdata;

import java.time.LocalDateTime;

public record MarketDataStatusResponse(
        boolean enabled,
        boolean apiKeyConfigured,
        LocalDateTime lastRunAt,
        MarketDataIngestionResult lastResult
) {
}
