package com.bonchang.qerp.marketdata;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.core.instrument.MeterRegistry;
import jakarta.annotation.PostConstruct;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class MarketDataStatusService {

    private final MarketDataRunRepository marketDataRunRepository;
    private final MarketQuoteRepository marketQuoteRepository;
    private final MarketDataProperties marketDataProperties;
    private final ObjectMapper objectMapper;
    private final MeterRegistry meterRegistry;
    private final AtomicInteger staleQuoteGauge = new AtomicInteger();

    @PostConstruct
    void registerGauge() {
        meterRegistry.gauge("qerp.marketdata.stale.quote.count", staleQuoteGauge);
    }

    public void recordRun(MarketDataIngestionResult result) {
        MarketDataRun run = new MarketDataRun();
        run.setStartedAt(result.startedAt());
        run.setFinishedAt(result.finishedAt());
        run.setSource(result.source());
        run.setTotalInstruments(result.totalInstruments());
        run.setSuccessCount(result.successCount());
        run.setFailureCount(result.failureCount());
        run.setRunStatus(result.runStatus());
        run.setLastQuoteReceivedAt(result.lastQuoteReceivedAt());
        run.setUpdatedSymbolsJson(writeList(result.updatedSymbols()));
        run.setFailureMessagesJson(writeList(result.failures()));
        run.setStaleInstrumentsJson(writeList(result.staleSymbols()));
        marketDataRunRepository.save(run);
        staleQuoteGauge.set(result.staleQuoteCount());
    }

    public MarketDataStatusResponse snapshot(boolean enabled, boolean apiKeyConfigured) {
        Optional<MarketDataRun> latestRun = marketDataRunRepository.findFirstByOrderByFinishedAtDescIdDesc();
        LocalDateTime lastQuoteReceivedAt = marketQuoteRepository.findFirstByOrderByReceivedAtDescIdDesc()
                .map(MarketQuote::getReceivedAt)
                .orElse(null);
        long staleQuoteCount = countStaleQuotes();
        staleQuoteGauge.set((int) staleQuoteCount);

        return new MarketDataStatusResponse(
                enabled,
                apiKeyConfigured,
                latestRun.map(MarketDataRun::getFinishedAt).orElse(null),
                lastQuoteReceivedAt,
                latestRun.map(MarketDataRun::getSource).orElse(marketDataProperties.getSourceMode()),
                staleQuoteCount > 0,
                staleQuoteCount,
                latestRun.map(this::toResult).orElse(null)
        );
    }

    public MarketDataHealthResponse health(boolean enabled, boolean apiKeyConfigured) {
        Optional<MarketDataRun> latestRun = marketDataRunRepository.findFirstByOrderByFinishedAtDescIdDesc();
        List<String> staleSymbols = marketQuoteRepository.findStaleQuotes(staleThreshold()).stream()
                .map(quote -> quote.getInstrument().getSymbol())
                .toList();
        long totalQuotes = marketQuoteRepository.count();
        String status = staleSymbols.isEmpty() ? "UP" : "DEGRADED";
        return new MarketDataHealthResponse(
                status,
                enabled,
                apiKeyConfigured,
                latestRun.map(MarketDataRun::getSource).orElse(marketDataProperties.getSourceMode()),
                latestRun.map(MarketDataRun::getFinishedAt).orElse(null),
                latestRun.map(MarketDataRun::getRunStatus).orElse("UNKNOWN"),
                marketQuoteRepository.findFirstByOrderByReceivedAtDescIdDesc().map(MarketQuote::getReceivedAt).orElse(null),
                totalQuotes,
                staleSymbols.size(),
                staleSymbols,
                latestRun.map(run -> readList(run.getFailureMessagesJson())).orElse(List.of())
        );
    }

    public List<MarketQuoteResponse> latestQuotes(int limit) {
        return marketQuoteRepository.findAllByOrderByReceivedAtDescIdDesc(PageRequest.of(0, Math.min(Math.max(limit, 1), 100)))
                .stream()
                .map(this::toQuoteResponse)
                .toList();
    }

    public MarketQuoteResponse latestQuoteBySymbol(String symbol) {
        return marketQuoteRepository.findByInstrumentSymbol(symbol)
                .map(this::toQuoteResponse)
                .orElseThrow(() -> new IllegalArgumentException("quote not found for symbol: " + symbol));
    }

    public boolean isQuoteStale(MarketQuote quote) {
        return isQuoteStale(quote.getReceivedAt());
    }

    public boolean isQuoteStale(LocalDateTime receivedAt) {
        return receivedAt != null && receivedAt.isBefore(staleThreshold());
    }

    public long countStaleQuotes() {
        return marketQuoteRepository.countByReceivedAtBefore(staleThreshold());
    }

    private LocalDateTime staleThreshold() {
        return LocalDateTime.now().minusSeconds(Math.max(1L, marketDataProperties.getStaleThresholdSeconds()));
    }

    private MarketDataIngestionResult toResult(MarketDataRun run) {
        return new MarketDataIngestionResult(
                run.getSource(),
                run.getStartedAt(),
                run.getFinishedAt(),
                run.getLastQuoteReceivedAt(),
                run.getTotalInstruments(),
                run.getSuccessCount(),
                run.getFailureCount(),
                readList(run.getStaleInstrumentsJson()).size(),
                run.getRunStatus(),
                readList(run.getUpdatedSymbolsJson()),
                readList(run.getStaleInstrumentsJson()),
                readList(run.getFailureMessagesJson())
        );
    }

    private MarketQuoteResponse toQuoteResponse(MarketQuote quote) {
        return new MarketQuoteResponse(
                quote.getInstrument().getId(),
                quote.getInstrument().getSymbol(),
                quote.getInstrument().getMarket(),
                quote.getQuoteTime(),
                quote.getLastPrice(),
                quote.getBidPrice(),
                quote.getAskPrice(),
                quote.getChangePercent(),
                quote.getSource(),
                quote.getReceivedAt(),
                isQuoteStale(quote)
        );
    }

    private String writeList(List<String> values) {
        try {
            return objectMapper.writeValueAsString(values == null ? List.of() : values);
        } catch (Exception ex) {
            throw new IllegalStateException("failed to serialize market data run payload", ex);
        }
    }

    private List<String> readList(String json) {
        if (json == null || json.isBlank()) {
            return Collections.emptyList();
        }
        try {
            return objectMapper.readValue(json, new TypeReference<>() {
            });
        } catch (Exception ex) {
            throw new IllegalStateException("failed to deserialize market data run payload", ex);
        }
    }
}
