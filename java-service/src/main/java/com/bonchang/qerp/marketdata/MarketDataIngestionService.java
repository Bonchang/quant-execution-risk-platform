package com.bonchang.qerp.marketdata;

import com.bonchang.qerp.instrument.Instrument;
import com.bonchang.qerp.instrument.InstrumentRepository;
import com.bonchang.qerp.market.MarketPrice;
import com.bonchang.qerp.market.MarketPriceRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
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
    private final MarketDataProperties marketDataProperties;
    private final MarketDataStatusService marketDataStatusService;
    private final RestTemplateBuilder restTemplateBuilder;

    @Scheduled(fixedDelayString = "${market-data.poll-ms:60000}")
    public void scheduledIngestion() {
        if (!marketDataProperties.isEnabled()) {
            return;
        }
        if (marketDataProperties.getApiKey() == null || marketDataProperties.getApiKey().isBlank()) {
            log.warn("market-data.enabled=true but market-data.api-key is empty; skipping scheduled ingestion");
            marketDataStatusService.update(
                    new MarketDataIngestionResult(0, 0, 0, List.of(), List.of("market-data.api-key is not configured"))
            );
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
        if (marketDataProperties.getApiKey() == null || marketDataProperties.getApiKey().isBlank()) {
            MarketDataIngestionResult result = new MarketDataIngestionResult(
                    0, 0, 0, List.of(), List.of("market-data.api-key is not configured")
            );
            marketDataStatusService.update(result);
            return result;
        }

        List<Instrument> instruments = instrumentRepository.findAll().stream()
                .sorted(Comparator.comparing(Instrument::getId))
                .limit(Math.max(1, marketDataProperties.getMaxInstrumentsPerRun()))
                .toList();

        int success = 0;
        int failure = 0;
        List<String> updatedSymbols = new ArrayList<>();
        List<String> failures = new ArrayList<>();

        RestTemplate restTemplate = restTemplateBuilder.build();

        for (Instrument instrument : instruments) {
            try {
                FinnhubQuoteResponse quote = fetchFinnhubQuote(restTemplate, instrument.getSymbol());
                if (!quote.hasValidOHLC()) {
                    failure++;
                    failures.add(instrument.getSymbol() + ": invalid quote payload");
                    continue;
                }

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
            } catch (Exception ex) {
                failure++;
                failures.add(instrument.getSymbol() + ": " + ex.getMessage());
                log.warn("failed to ingest quote for {}", instrument.getSymbol(), ex);
            }
        }

        MarketDataIngestionResult result = new MarketDataIngestionResult(
                instruments.size(), success, failure, updatedSymbols, failures
        );
        marketDataStatusService.update(result);
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

    private static final class FinnhubQuoteResponse {
        public Double c;
        public Double h;
        public Double l;
        public Double o;
        public Long t;

        boolean hasValidOHLC() {
            return c != null && h != null && l != null && o != null;
        }

        LocalDate resolvePriceDate() {
            if (t == null || t <= 0) {
                return LocalDate.now(ZoneOffset.UTC);
            }
            return Instant.ofEpochSecond(t).atZone(ZoneOffset.UTC).toLocalDate();
        }

        BigDecimal toBigDecimal(Double value) {
            return BigDecimal.valueOf(value).setScale(6, RoundingMode.HALF_UP);
        }
    }
}
