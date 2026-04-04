package com.bonchang.qerp.order;

import com.bonchang.qerp.market.MarketPrice;
import com.bonchang.qerp.market.MarketPriceRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class OrderPricingService {

    private final MarketPriceRepository marketPriceRepository;

    @Value("${execution.default-fill-price:1.000000}")
    private BigDecimal defaultFillPrice;

    public Optional<BigDecimal> resolveLatestMarketPrice(Order order) {
        return marketPriceRepository.findFirstByInstrumentIdOrderByPriceDateDescIdDesc(order.getInstrument().getId())
                .map(MarketPrice::getClosePrice)
                .map(this::scale);
    }

    public BigDecimal resolveExecutionPriceOrDefault(Order order) {
        return resolveLatestMarketPrice(order).orElse(scale(defaultFillPrice));
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

    private BigDecimal scale(BigDecimal value) {
        return value.setScale(6, RoundingMode.HALF_UP);
    }
}
