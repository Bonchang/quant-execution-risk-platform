package com.bonchang.qerp.order;

import com.bonchang.qerp.market.MarketPrice;
import com.bonchang.qerp.market.MarketPriceRepository;
import com.bonchang.qerp.marketdata.MarketDataProperties;
import com.bonchang.qerp.marketdata.MarketDataStatusService;
import com.bonchang.qerp.marketdata.MarketQuote;
import com.bonchang.qerp.marketdata.MarketQuoteRepository;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class OrderPricingService {

    private final MarketPriceRepository marketPriceRepository;
    private final MarketQuoteRepository marketQuoteRepository;
    private final MarketDataStatusService marketDataStatusService;
    private final MarketDataProperties marketDataProperties;
    private final MeterRegistry meterRegistry;

    @Value("${execution.default-fill-price:1.000000}")
    private BigDecimal defaultFillPrice;

    public Optional<BigDecimal> resolveLatestMarketPrice(Order order) {
        return resolveMarkPrice(order.getInstrument().getId());
    }

    public Optional<BigDecimal> resolveMarkPrice(Long instrumentId) {
        Optional<MarketQuote> quote = marketQuoteRepository.findByInstrumentId(instrumentId);
        if (quote.isPresent()) {
            return Optional.of(scale(quote.get().getLastPrice()));
        }
        incrementFallback("mark_price_close");
        return marketPriceRepository.findFirstByInstrumentIdOrderByPriceDateDescIdDesc(instrumentId)
                .map(MarketPrice::getClosePrice)
                .map(this::scale);
    }

    public BigDecimal resolveExecutionPriceOrDefault(Order order) {
        return resolveExecutionPrice(order).orElseGet(() -> {
            incrementFallback("default_fill_price");
            return scale(defaultFillPrice);
        });
    }

    public Optional<BigDecimal> resolveExecutionPrice(Order order) {
        Optional<PriceSnapshot> snapshot = resolvePriceSnapshot(order.getInstrument().getId());
        if (snapshot.isPresent() && snapshot.get().quoteAvailable()) {
            BigDecimal raw = order.getSide() == OrderSide.BUY
                    ? snapshot.get().askPrice()
                    : snapshot.get().bidPrice();
            return Optional.of(applySlippage(raw, order.getSide()));
        }
        incrementFallback("execution_close_price");
        return marketPriceRepository.findFirstByInstrumentIdOrderByPriceDateDescIdDesc(order.getInstrument().getId())
                .map(MarketPrice::getClosePrice)
                .map(this::scale);
    }

    public BigDecimal estimateReservationPrice(Order order) {
        if (order.getOrderType() == OrderType.LIMIT && order.getLimitPrice() != null) {
            return scale(order.getLimitPrice());
        }
        return resolveExecutionPriceOrDefault(order);
    }

    public BigDecimal calculateEstimatedBuyNotional(Order order) {
        return scale(order.getQuantity().multiply(estimateReservationPrice(order)));
    }

    public Optional<BigDecimal> resolveLimitReferencePrice(Order order) {
        Optional<PriceSnapshot> snapshot = resolvePriceSnapshot(order.getInstrument().getId());
        if (snapshot.isPresent() && snapshot.get().quoteAvailable()) {
            BigDecimal raw = order.getSide() == OrderSide.BUY
                    ? snapshot.get().askPrice()
                    : snapshot.get().bidPrice();
            return Optional.of(scale(raw));
        }
        incrementFallback("limit_close_price");
        return marketPriceRepository.findFirstByInstrumentIdOrderByPriceDateDescIdDesc(order.getInstrument().getId())
                .map(MarketPrice::getClosePrice)
                .map(this::scale);
    }

    public Optional<PriceSnapshot> resolvePriceSnapshot(Long instrumentId) {
        return marketQuoteRepository.findByInstrumentId(instrumentId)
                .map(quote -> new PriceSnapshot(
                        scale(quote.getLastPrice()),
                        scale(quote.getBidPrice()),
                        scale(quote.getAskPrice()),
                        true,
                        marketDataStatusService.isQuoteStale(quote),
                        false,
                        quote.getSource(),
                        quote.getQuoteTime(),
                        quote.getReceivedAt()
                ))
                .or(() -> marketPriceRepository.findFirstByInstrumentIdOrderByPriceDateDescIdDesc(instrumentId)
                        .map(price -> new PriceSnapshot(
                                scale(price.getClosePrice()),
                                scale(price.getClosePrice()),
                                scale(price.getClosePrice()),
                                false,
                                false,
                                true,
                                "MARKET_PRICE_CLOSE",
                                price.getPriceDate().atStartOfDay(),
                                null
                        )));
    }

    private BigDecimal applySlippage(BigDecimal price, OrderSide side) {
        BigDecimal slippageBps = marketDataProperties.getExecutionSlippageBps() == null
                ? BigDecimal.ZERO
                : marketDataProperties.getExecutionSlippageBps();
        BigDecimal factor = BigDecimal.ONE.add(
                (side == OrderSide.BUY ? slippageBps : slippageBps.negate())
                        .divide(new BigDecimal("10000"), 8, RoundingMode.HALF_UP)
        );
        return scale(price.multiply(factor));
    }

    private void incrementFallback(String reason) {
        Counter.builder("qerp.order.pricing.fallback.count")
                .tag("reason", reason)
                .register(meterRegistry)
                .increment();
    }

    private BigDecimal scale(BigDecimal value) {
        return value.setScale(6, RoundingMode.HALF_UP);
    }

    public record PriceSnapshot(
            BigDecimal lastPrice,
            BigDecimal bidPrice,
            BigDecimal askPrice,
            boolean quoteAvailable,
            boolean stale,
            boolean fallbackToClose,
            String source,
            LocalDateTime quoteTime,
            LocalDateTime receivedAt
    ) {
    }
}
