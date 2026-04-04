package com.bonchang.qerp.marketdata;

import lombok.RequiredArgsConstructor;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import java.util.List;

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

    @GetMapping("/health")
    public MarketDataHealthResponse health() {
        return marketDataStatusService.health(
                marketDataProperties.isEnabled(),
                marketDataProperties.getApiKey() != null && !marketDataProperties.getApiKey().isBlank()
        );
    }

    @GetMapping("/quotes")
    public List<MarketQuoteResponse> quotes() {
        return marketDataStatusService.latestQuotes(50);
    }

    @GetMapping("/quotes/{symbol}")
    public MarketQuoteResponse quoteBySymbol(@PathVariable String symbol) {
        try {
            return marketDataStatusService.latestQuoteBySymbol(symbol);
        } catch (IllegalArgumentException ex) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, ex.getMessage(), ex);
        }
    }

    @PostMapping("/ingest")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public MarketDataIngestionResult ingestNow() {
        return marketDataIngestionService.ingestLatestPrices();
    }
}
