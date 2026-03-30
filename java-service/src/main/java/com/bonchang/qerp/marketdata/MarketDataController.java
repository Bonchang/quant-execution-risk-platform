package com.bonchang.qerp.marketdata;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/market-data")
@RequiredArgsConstructor
public class MarketDataController {

    private final MarketDataIngestionService marketDataIngestionService;
    private final MarketDataStatusService marketDataStatusService;
    private final MarketDataProperties marketDataProperties;

    @GetMapping("/status")
    public MarketDataStatusResponse status() {
        return marketDataStatusService.snapshot(
                marketDataProperties.isEnabled(),
                marketDataProperties.getApiKey() != null && !marketDataProperties.getApiKey().isBlank()
        );
    }

    @PostMapping("/ingest")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public MarketDataIngestionResult ingestNow() {
        return marketDataIngestionService.ingestLatestPrices();
    }
}
