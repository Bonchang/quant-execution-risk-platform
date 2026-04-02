package com.bonchang.qerp.execution;

import com.bonchang.qerp.fill.Fill;
import com.bonchang.qerp.fill.FillRepository;
import com.bonchang.qerp.market.MarketPrice;
import com.bonchang.qerp.market.MarketPriceRepository;
import com.bonchang.qerp.order.Order;
import com.bonchang.qerp.order.OrderType;
import com.bonchang.qerp.order.OrderRepository;
import com.bonchang.qerp.order.OrderSide;
import com.bonchang.qerp.order.OrderStatus;
import com.bonchang.qerp.position.Position;
import com.bonchang.qerp.position.PositionRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class OrderExecutionService {

    private static final BigDecimal ZERO = BigDecimal.ZERO.setScale(6, RoundingMode.HALF_UP);

    private final MarketPriceRepository marketPriceRepository;
    private final FillRepository fillRepository;
    private final PositionRepository positionRepository;
    private final OrderRepository orderRepository;

    @Value("${execution.default-fill-price:1.000000}")
    private BigDecimal defaultFillPrice;

    public Order executeApprovedOrder(Order order) {
        if (order.getStatus() != OrderStatus.APPROVED) {
            return order;
        }

        BigDecimal fillPrice = resolveFillPrice(order);
        List<BigDecimal> plan = buildExecutionPlan(order);
        if (plan.isEmpty()) {
            order.setUpdatedAt(LocalDateTime.now());
            return orderRepository.save(order);
        }

        for (BigDecimal fillQuantity : plan) {
            applyFill(order, fillPrice, fillQuantity);
        }
        updateOrderStatusAfterExecution(order);
        order.setUpdatedAt(LocalDateTime.now());
        return orderRepository.save(order);
    }

    private List<BigDecimal> buildExecutionPlan(Order order) {
        if (order.getOrderType() == OrderType.MARKET) {
            return marketExecutionPlan(order);
        }
        return limitExecutionPlan(order);
    }

    private List<BigDecimal> marketExecutionPlan(Order order) {
        BigDecimal remaining = sanitizeScale(order.getRemainingQuantity());
        if (remaining.compareTo(ZERO) <= 0) {
            return List.of();
        }

        List<BigDecimal> chunks = new ArrayList<>();
        BigDecimal firstChunk = remaining.divide(BigDecimal.valueOf(2), 6, RoundingMode.HALF_UP);
        if (firstChunk.compareTo(ZERO) > 0) {
            chunks.add(firstChunk);
        }
        BigDecimal secondChunk = remaining.subtract(firstChunk).setScale(6, RoundingMode.HALF_UP);
        if (secondChunk.compareTo(ZERO) > 0) {
            chunks.add(secondChunk);
        }
        return chunks;
    }

    private List<BigDecimal> limitExecutionPlan(Order order) {
        BigDecimal remaining = sanitizeScale(order.getRemainingQuantity());
        if (remaining.compareTo(ZERO) <= 0) {
            return List.of();
        }

        BigDecimal partial = remaining.multiply(new BigDecimal("0.500000"))
                .setScale(6, RoundingMode.HALF_UP);
        if (partial.compareTo(ZERO) <= 0) {
            return List.of();
        }
        return List.of(partial);
    }

    private BigDecimal resolveFillPrice(Order order) {
        Optional<MarketPrice> latest = marketPriceRepository.findFirstByInstrumentIdOrderByPriceDateDescIdDesc(
                order.getInstrument().getId()
        );
        return latest.map(MarketPrice::getClosePrice).orElse(defaultFillPrice);
    }

    private void applyFill(Order order, BigDecimal fillPrice, BigDecimal fillQuantity) {
        if (fillQuantity.compareTo(ZERO) <= 0) {
            return;
        }

        Fill fill = new Fill();
        fill.setOrder(order);
        fill.setStrategyRun(order.getStrategyRun());
        fill.setInstrument(order.getInstrument());
        fill.setFillQuantity(fillQuantity);
        fill.setFillPrice(fillPrice);
        fill.setFilledAt(LocalDateTime.now());
        fillRepository.save(fill);

        BigDecimal currentFilled = sanitizeScale(order.getFilledQuantity());
        BigDecimal nextFilled = currentFilled.add(fillQuantity).setScale(6, RoundingMode.HALF_UP);
        BigDecimal requested = sanitizeScale(order.getQuantity());
        BigDecimal remaining = requested.subtract(nextFilled).max(BigDecimal.ZERO).setScale(6, RoundingMode.HALF_UP);
        order.setFilledQuantity(nextFilled);
        order.setRemainingQuantity(remaining);
        order.setLastExecutedAt(fill.getFilledAt());

        upsertPosition(order, fillPrice, fillQuantity);
    }

    private void upsertPosition(Order order, BigDecimal fillPrice, BigDecimal fillQuantity) {
        Position position = positionRepository.findByStrategyRunIdAndInstrumentId(
                order.getStrategyRun().getId(),
                order.getInstrument().getId()
        ).orElseGet(() -> {
            Position newPosition = new Position();
            newPosition.setStrategyRun(order.getStrategyRun());
            newPosition.setInstrument(order.getInstrument());
            newPosition.setNetQuantity(ZERO);
            newPosition.setAveragePrice(ZERO);
            newPosition.setUpdatedAt(LocalDateTime.now());
            return newPosition;
        });

        BigDecimal existingQuantity = position.getNetQuantity();

        if (order.getSide() == OrderSide.BUY) {
            BigDecimal newQuantity = existingQuantity.add(fillQuantity);
            BigDecimal weightedCost = existingQuantity.multiply(position.getAveragePrice())
                    .add(fillQuantity.multiply(fillPrice));
            BigDecimal newAveragePrice = ZERO;
            if (newQuantity.compareTo(BigDecimal.ZERO) > 0) {
                newAveragePrice = weightedCost.divide(newQuantity, 6, RoundingMode.HALF_UP);
            }
            position.setNetQuantity(newQuantity.setScale(6, RoundingMode.HALF_UP));
            position.setAveragePrice(newAveragePrice);
        } else {
            BigDecimal newQuantity = existingQuantity.subtract(fillQuantity);
            position.setNetQuantity(newQuantity.setScale(6, RoundingMode.HALF_UP));
            if (newQuantity.compareTo(BigDecimal.ZERO) <= 0) {
                position.setAveragePrice(ZERO);
            }
        }

        position.setUpdatedAt(LocalDateTime.now());
        positionRepository.save(position);
    }

    private void updateOrderStatusAfterExecution(Order order) {
        BigDecimal requested = sanitizeScale(order.getQuantity());
        BigDecimal filled = sanitizeScale(order.getFilledQuantity());
        if (filled.compareTo(ZERO) <= 0) {
            order.setStatus(OrderStatus.APPROVED);
            return;
        }
        if (filled.compareTo(requested) >= 0) {
            order.setStatus(OrderStatus.FILLED);
            return;
        }
        order.setStatus(OrderStatus.PARTIALLY_FILLED);
    }

    private BigDecimal sanitizeScale(BigDecimal value) {
        if (value == null) {
            return ZERO;
        }
        return value.setScale(6, RoundingMode.HALF_UP);
    }
}
