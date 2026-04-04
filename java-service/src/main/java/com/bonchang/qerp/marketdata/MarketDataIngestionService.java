package com.bonchang.qerp.marketdata;

import com.bonchang.qerp.execution.OrderExecutionService;
import com.bonchang.qerp.instrument.Instrument;
import com.bonchang.qerp.instrument.InstrumentRepository;
import com.bonchang.qerp.market.MarketPrice;
import com.bonchang.qerp.market.MarketPriceRepository;
import com.bonchang.qerp.outbox.OutboxEventService;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

@Service
@RequiredArgsConstructor
@Slf4j
public class MarketDataIngestionService {

    private final InstrumentRepository instrumentRepository;
    private final MarketPriceRepository marketPriceRepository;
    private final MarketQuoteRepository marketQuoteRepository;
    private final MarketDataProperties marketDataProperties;
    private final MarketDataStatusService marketDataStatusService;
    private final OutboxEventService outboxEventService;
    private final OrderExecutionService orderExecutionService;
    private final RestTemplateBuilder restTemplateBuilder;
    private final MeterRegistry meterRegistry;

    @Scheduled(fixedDelayString = "${market-data.poll-ms:60000}")
    public void scheduledIngestion() {
        if (!marketDataProperties.isEnabled()) {
            return;
        }
        MarketDataIngestionResult result = ingestLatestPrices();
        log.info(
                "market data ingestion finished: total={}, success={}, failure={}",
                result.totalInstruments(), result.successCount(), result.failureCount()
        );
    }

    @Transactional
    public MarketDataIngestionResult ingestLatestPrices() {
        LocalDateTime startedAt = LocalDateTime.now();
        List<Instrument> instruments = instrumentRepository.findAll().stream()
                .sorted(Comparator.comparing(Instrument::getId))
                .limit(Math.max(1, marketDataProperties.getMaxInstrumentsPerRun()))
                .toList();

        if (useDemoMode()) {
            return ingestDemoQuotes(startedAt, instruments);
        }
        if (marketDataProperties.getApiKey() == null || marketDataProperties.getApiKey().isBlank()) {
            MarketDataIngestionResult result = new MarketDataIngestionResult(
                    marketDataProperties.getSourceMode(),
                    startedAt,
                    LocalDateTime.now(),
                    null,
                    instruments.size(),
                    0,
                    instruments.size(),
                    (int) marketDataStatusService.countStaleQuotes(),
                    "FAILED",
                    List.of(),
                    staleSymbols(),
                    List.of("market-data.api-key is not configured")
            );
            marketDataStatusService.recordRun(result);
            publishRunFailedEvent(result);
            return result;
        }

        int success = 0;
        int failure = 0;
        List<String> updatedSymbols = new ArrayList<>();
        List<String> failures = new ArrayList<>();
        LocalDateTime lastQuoteReceivedAt = null;

        RestTemplate restTemplate = restTemplateBuilder.build();
        Timer.Sample sample = Timer.start(meterRegistry);

        for (Instrument instrument : instruments) {
            try {
                FinnhubQuoteResponse quote = fetchFinnhubQuote(restTemplate, instrument.getSymbol());
                if (!quote.hasValidQuote()) {
                    failure++;
                    failures.add(instrument.getSymbol() + ": invalid quote payload");
                    incrementFailureCounter("invalid_payload");
                    continue;
                }

                LocalDateTime quoteTime = quote.resolveQuoteTime();
                LocalDateTime receivedAt = LocalDateTime.now();
                lastQuoteReceivedAt = receivedAt;

                MarketQuote marketQuote = marketQuoteRepository.findByInstrumentId(instrument.getId())
                        .orElseGet(MarketQuote::new);
                marketQuote.setInstrument(instrument);
                marketQuote.setQuoteTime(quoteTime);
                marketQuote.setLastPrice(quote.toBigDecimal(quote.c));
                marketQuote.setBidPrice(quote.resolveBidPrice(marketDataProperties.getSyntheticSpreadBps()));
                marketQuote.setAskPrice(quote.resolveAskPrice(marketDataProperties.getSyntheticSpreadBps()));
                marketQuote.setChangePercent(quote.resolveChangePercent());
                marketQuote.setSource(marketDataProperties.getSourceMode());
                marketQuote.setReceivedAt(receivedAt);
                MarketQuote savedQuote = marketQuoteRepository.save(marketQuote);

                LocalDate priceDate = quote.resolvePriceDate();
                MarketPrice marketPrice = marketPriceRepository
                        .findByInstrumentIdAndPriceDate(instrument.getId(), priceDate)
                        .orElseGet(MarketPrice::new);

                marketPrice.setInstrument(instrument);
                marketPrice.setPriceDate(priceDate);
                marketPrice.setOpenPrice(quote.toBigDecimal(quote.o));
                marketPrice.setHighPrice(quote.toBigDecimal(quote.h));
                marketPrice.setLowPrice(quote.toBigDecimal(quote.l));
                marketPrice.setClosePrice(quote.toBigDecimal(quote.c));
                marketPrice.setVolume(0L);

                marketPriceRepository.save(marketPrice);
                success++;
                updatedSymbols.add(instrument.getSymbol());
                publishQuoteUpdatedEvent(savedQuote, instrument.getId(), instrument.getSymbol());
                orderExecutionService.reevaluateWorkingOrdersForInstrument(instrument.getId());
            } catch (Exception ex) {
                failure++;
                failures.add(instrument.getSymbol() + ": " + ex.getMessage());
                incrementFailureCounter("fetch_exception");
                log.warn("failed to ingest quote for {}", instrument.getSymbol(), ex);
            }
        }

        List<String> staleSymbols = staleSymbols();
        for (String staleSymbol : staleSymbols) {
            publishQuoteStaleEvent(staleSymbol);
        }

        sample.stop(Timer.builder("qerp.marketdata.quote.update.latency").register(meterRegistry));
        MarketDataIngestionResult result = new MarketDataIngestionResult(
                marketDataProperties.getSourceMode(),
                startedAt,
                LocalDateTime.now(),
                lastQuoteReceivedAt,
                instruments.size(),
                success,
                failure,
                staleSymbols.size(),
                failure > 0 ? (success > 0 ? "PARTIAL_SUCCESS" : "FAILED") : "SUCCESS",
                updatedSymbols,
                staleSymbols,
                failures
        );
        marketDataStatusService.recordRun(result);
        if (failure > 0) {
            publishRunFailedEvent(result);
        }
        return result;
    }

    private MarketDataIngestionResult ingestDemoQuotes(LocalDateTime startedAt, List<Instrument> instruments) {
        int success = 0;
        List<String> updatedSymbols = new ArrayList<>();
        LocalDateTime lastQuoteReceivedAt = null;
        Timer.Sample sample = Timer.start(meterRegistry);

        for (Instrument instrument : instruments) {
            try {
                LocalDateTime quoteTime = LocalDateTime.now();
                MarketPrice referencePrice = marketPriceRepository.findFirstByInstrumentIdOrderByPriceDateDescIdDesc(instrument.getId())
                        .orElseThrow(() -> new IllegalStateException("reference price not found"));
                BigDecimal lastPrice = generateSyntheticLastPrice(referencePrice.getClosePrice(), instrument.getId(), quoteTime);
                BigDecimal bidPrice = applySpread(lastPrice, false);
                BigDecimal askPrice = applySpread(lastPrice, true);
                BigDecimal changePercent = lastPrice.subtract(referencePrice.getClosePrice())
                        .divide(referencePrice.getClosePrice(), 8, RoundingMode.HALF_UP)
                        .multiply(new BigDecimal("100"))
                        .setScale(6, RoundingMode.HALF_UP);

                MarketQuote marketQuote = marketQuoteRepository.findByInstrumentId(instrument.getId()).orElseGet(MarketQuote::new);
                marketQuote.setInstrument(instrument);
                marketQuote.setQuoteTime(quoteTime);
                marketQuote.setLastPrice(lastPrice);
                marketQuote.setBidPrice(bidPrice);
                marketQuote.setAskPrice(askPrice);
                marketQuote.setChangePercent(changePercent);
                marketQuote.setSource("DEMO_SYNTHETIC");
                marketQuote.setReceivedAt(quoteTime);
                MarketQuote savedQuote = marketQuoteRepository.save(marketQuote);

                success++;
                updatedSymbols.add(instrument.getSymbol());
                lastQuoteReceivedAt = quoteTime;
                publishQuoteUpdatedEvent(savedQuote, instrument.getId(), instrument.getSymbol());
                orderExecutionService.reevaluateWorkingOrdersForInstrument(instrument.getId());
            } catch (Exception ex) {
                incrementFailureCounter("demo_generation_exception");
                log.warn("failed to synthesize quote for {}", instrument.getSymbol(), ex);
            }
        }

        List<String> staleSymbols = staleSymbols();
        for (String staleSymbol : staleSymbols) {
            publishQuoteStaleEvent(staleSymbol);
        }

        sample.stop(Timer.builder("qerp.marketdata.quote.update.latency").register(meterRegistry));
        MarketDataIngestionResult result = new MarketDataIngestionResult(
                "DEMO_SYNTHETIC",
                startedAt,
                LocalDateTime.now(),
                lastQuoteReceivedAt,
                instruments.size(),
                success,
                instruments.size() - success,
                staleSymbols.size(),
                success == instruments.size() ? "SUCCESS" : "PARTIAL_SUCCESS",
                updatedSymbols,
                staleSymbols,
                List.of()
        );
        marketDataStatusService.recordRun(result);
        return result;
    }

    private FinnhubQuoteResponse fetchFinnhubQuote(RestTemplate restTemplate, String symbol) {
        String uri = UriComponentsBuilder.fromUriString(marketDataProperties.getBaseUrl())
                .path("/quote")
                .queryParam("symbol", symbol)
                .queryParam("token", marketDataProperties.getApiKey())
                .build()
                .toUriString();

        ResponseEntity<FinnhubQuoteResponse> response = restTemplate.getForEntity(uri, FinnhubQuoteResponse.class);
        FinnhubQuoteResponse body = response.getBody();
        if (body == null) {
            throw new IllegalStateException("empty response");
        }
        return body;
    }

    private void publishQuoteUpdatedEvent(MarketQuote quote, Long instrumentId, String symbol) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("instrumentId", instrumentId);
        payload.put("symbol", symbol);
        payload.put("lastPrice", quote.getLastPrice());
        payload.put("bidPrice", quote.getBidPrice());
        payload.put("askPrice", quote.getAskPrice());
        payload.put("receivedAt", quote.getReceivedAt());
        outboxEventService.publishMarketDataEvent("MARKET_QUOTE", instrumentId, "QUOTE_UPDATED", payload);
    }

    private void publishQuoteStaleEvent(String symbol) {
        Instrument instrument = instrumentRepository.findBySymbol(symbol).orElse(null);
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("symbol", symbol);
        payload.put("staleThresholdSeconds", marketDataProperties.getStaleThresholdSeconds());
        outboxEventService.publishMarketDataEvent(
                "MARKET_QUOTE",
                instrument != null ? instrument.getId() : 0L,
                "QUOTE_STALE",
                payload
        );
    }

    private void publishRunFailedEvent(MarketDataIngestionResult result) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("source", result.source());
        payload.put("runStatus", result.runStatus());
        payload.put("failureCount", result.failureCount());
        payload.put("failures", result.failures());
        payload.put("finishedAt", result.finishedAt());
        outboxEventService.publishMarketDataEvent("MARKET_DATA_RUN", 0L, "MARKET_DATA_RUN_FAILED", payload);
    }

    private List<String> staleSymbols() {
        return marketQuoteRepository.findStaleQuotes(LocalDateTime.now()
                        .minusSeconds(Math.max(1L, marketDataProperties.getStaleThresholdSeconds())))
                .stream()
                .map(quote -> quote.getInstrument().getSymbol())
                .toList();
    }

    private boolean useDemoMode() {
        return marketDataProperties.getSourceMode() == null
                || marketDataProperties.getSourceMode().isBlank()
                || "DEMO_SYNTHETIC".equalsIgnoreCase(marketDataProperties.getSourceMode())
                || (marketDataProperties.getApiKey() == null || marketDataProperties.getApiKey().isBlank());
    }

    private BigDecimal generateSyntheticLastPrice(BigDecimal baseClose, Long instrumentId, LocalDateTime quoteTime) {
        long bucket = quoteTime.toEpochSecond(ZoneOffset.UTC) / Math.max(1L, marketDataProperties.getPollMs() / 1000L);
        double wave = Math.sin((bucket + instrumentId) * 0.65d) * 0.006d
                + Math.cos((bucket + instrumentId) * 0.21d) * 0.003d;
        BigDecimal multiplier = BigDecimal.ONE.add(BigDecimal.valueOf(wave));
        return baseClose.multiply(multiplier).setScale(6, RoundingMode.HALF_UP);
    }

    private BigDecimal applySpread(BigDecimal lastPrice, boolean ask) {
        BigDecimal spreadFactor = marketDataProperties.getSyntheticSpreadBps()
                .divide(new BigDecimal("20000"), 8, RoundingMode.HALF_UP);
        BigDecimal factor = ask
                ? BigDecimal.ONE.add(spreadFactor)
                : BigDecimal.ONE.subtract(spreadFactor);
        return lastPrice.multiply(factor).setScale(6, RoundingMode.HALF_UP);
    }

    private void incrementFailureCounter(String reason) {
        Counter.builder("qerp.marketdata.failure.count")
                .tag("reason", reason)
                .register(meterRegistry)
                .increment();
    }

    private static final class FinnhubQuoteResponse {
        public Double c;
        public Double d;
        public Double dp;
        public Double h;
        public Double l;
        public Double o;
        public Double pc;
        public Long t;

        boolean hasValidQuote() {
            return c != null && h != null && l != null && o != null && c > 0d;
        }

        LocalDate resolvePriceDate() {
            if (t == null || t <= 0) {
                return LocalDate.now(ZoneOffset.UTC);
            }
            return Instant.ofEpochSecond(t).atZone(ZoneOffset.UTC).toLocalDate();
        }

        LocalDateTime resolveQuoteTime() {
            if (t == null || t <= 0) {
                return LocalDateTime.now(ZoneOffset.UTC);
            }
            return Instant.ofEpochSecond(t).atZone(ZoneOffset.UTC).toLocalDateTime();
        }

        BigDecimal toBigDecimal(Double value) {
            return BigDecimal.valueOf(value).setScale(6, RoundingMode.HALF_UP);
        }

        BigDecimal resolveBidPrice(BigDecimal spreadBps) {
            BigDecimal mid = toBigDecimal(c);
            BigDecimal halfSpread = normalizeSpread(spreadBps).divide(new BigDecimal("2"), 8, RoundingMode.HALF_UP);
            BigDecimal factor = BigDecimal.ONE.subtract(halfSpread.divide(new BigDecimal("10000"), 8, RoundingMode.HALF_UP));
            return mid.multiply(factor).setScale(6, RoundingMode.HALF_UP);
        }

        BigDecimal resolveAskPrice(BigDecimal spreadBps) {
            BigDecimal mid = toBigDecimal(c);
            BigDecimal halfSpread = normalizeSpread(spreadBps).divide(new BigDecimal("2"), 8, RoundingMode.HALF_UP);
            BigDecimal factor = BigDecimal.ONE.add(halfSpread.divide(new BigDecimal("10000"), 8, RoundingMode.HALF_UP));
            return mid.multiply(factor).setScale(6, RoundingMode.HALF_UP);
        }

        BigDecimal resolveChangePercent() {
            if (dp != null) {
                return toBigDecimal(dp);
            }
            if (pc == null || pc == 0d) {
                return BigDecimal.ZERO.setScale(6, RoundingMode.HALF_UP);
            }
            BigDecimal close = toBigDecimal(c);
            BigDecimal previousClose = toBigDecimal(pc);
            return close.subtract(previousClose)
                    .divide(previousClose, 8, RoundingMode.HALF_UP)
                    .multiply(new BigDecimal("100"))
                    .setScale(6, RoundingMode.HALF_UP);
        }

        private BigDecimal normalizeSpread(BigDecimal spreadBps) {
            if (spreadBps == null || spreadBps.compareTo(BigDecimal.ZERO) < 0) {
                return BigDecimal.ZERO.setScale(6, RoundingMode.HALF_UP);
            }
            return spreadBps.setScale(6, RoundingMode.HALF_UP);
        }
    }
}
