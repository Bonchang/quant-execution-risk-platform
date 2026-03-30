package com.bonchang.qerp.marketdata;

import java.time.LocalDateTime;
import org.springframework.stereotype.Service;

@Service
public class MarketDataStatusService {

    private LocalDateTime lastRunAt;
    private MarketDataIngestionResult lastResult;

    public synchronized void update(MarketDataIngestionResult result) {
        this.lastRunAt = LocalDateTime.now();
        this.lastResult = result;
    }

    public synchronized MarketDataStatusResponse snapshot(boolean enabled, boolean apiKeyConfigured) {
        return new MarketDataStatusResponse(enabled, apiKeyConfigured, lastRunAt, lastResult);
    }
}
