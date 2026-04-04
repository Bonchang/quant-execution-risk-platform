package com.bonchang.qerp.marketdata;

import java.time.LocalDateTime;
import java.util.List;

public record MarketDataIngestionResult(
        String source,
        LocalDateTime startedAt,
        LocalDateTime finishedAt,
        LocalDateTime lastQuoteReceivedAt,
        int totalInstruments,
        int successCount,
        int failureCount,
        int staleQuoteCount,
        String runStatus,
        List<String> updatedSymbols,
        List<String> staleSymbols,
        List<String> failures
) {
}
