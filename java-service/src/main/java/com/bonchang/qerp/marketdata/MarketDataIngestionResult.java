package com.bonchang.qerp.marketdata;

import java.util.List;

public record MarketDataIngestionResult(
        int totalInstruments,
        int successCount,
        int failureCount,
        List<String> updatedSymbols,
        List<String> failures
) {
}
