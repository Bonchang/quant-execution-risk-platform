package com.bonchang.qerp.execution;

import com.bonchang.qerp.account.AccountService;
import com.bonchang.qerp.fill.Fill;
import com.bonchang.qerp.fill.FillRepository;
import com.bonchang.qerp.order.Order;
import com.bonchang.qerp.order.OrderPricingService;
import com.bonchang.qerp.order.OrderRepository;
import com.bonchang.qerp.order.OrderSide;
import com.bonchang.qerp.order.OrderStatus;
import com.bonchang.qerp.order.OrderType;
import com.bonchang.qerp.outbox.OutboxEventService;
import com.bonchang.qerp.position.Position;
import com.bonchang.qerp.position.PositionRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class OrderExecutionService {

    private static final BigDecimal ZERO = BigDecimal.ZERO.setScale(6, RoundingMode.HALF_UP);

    private final FillRepository fillRepository;
    private final PositionRepository positionRepository;
    private final OrderRepository orderRepository;
    private final OrderPricingService orderPricingService;
    private final AccountService accountService;
    private final OutboxEventService outboxEventService;

    @Transactional
    public Order executeApprovedOrder(Order order) {
        if (order.getStatus() != OrderStatus.APPROVED) {
            return order;
        }

        if (order.getOrderType() == OrderType.MARKET) {
            return executeMarketOrder(order);
        }
        return executeLimitOrder(order);
    }

    @Transactional
    public int reevaluateWorkingOrdersForInstrument(Long instrumentId) {
        int executed = 0;
        for (Order order : orderRepository.findByStatusAndInstrumentIdOrderByIdAsc(OrderStatus.WORKING, instrumentId)) {
            Order updated = executeWorkingLimitOrder(order);
            if (updated.getStatus() == OrderStatus.FILLED) {
                executed++;
            }
        }
        return executed;
    }

    private Order executeMarketOrder(Order order) {
        BigDecimal fillPrice = orderPricingService.resolveExecutionPriceOrDefault(order);
        List<BigDecimal> plan = List.of(sanitizeScale(order.getRemainingQuantity()));
        if (plan.isEmpty()) {
            order.setUpdatedAt(LocalDateTime.now());
            Order saved = orderRepository.save(order);
            outboxEventService.publishOrderEvent(saved, "ORDER_APPROVED");
            return saved;
        }

        for (BigDecimal fillQuantity : plan) {
            applyFill(order, fillPrice, fillQuantity);
        }
        updateOrderStatusAfterExecution(order);
        finalizeOrderReservation(order);
        order.setUpdatedAt(LocalDateTime.now());
        Order savedOrder = orderRepository.save(order);
        outboxEventService.publishOrderEvent(savedOrder, savedOrder.getStatus() == OrderStatus.FILLED
                ? "ORDER_FILLED"
                : "ORDER_PARTIALLY_FILLED");
        return savedOrder;
    }

    private Order executeWorkingLimitOrder(Order order) {
        if (order.getStatus() != OrderStatus.WORKING) {
            return order;
        }
        return executeLimitOrder(order);
    }

    private Order executeLimitOrder(Order order) {
        Optional<BigDecimal> currentPrice = orderPricingService.resolveLimitReferencePrice(order);
        if (currentPrice.isEmpty() || !isExecutableLimitOrder(order, currentPrice.get())) {
            order.setStatus(OrderStatus.WORKING);
            order.setUpdatedAt(LocalDateTime.now());
            Order saved = orderRepository.save(order);
            outboxEventService.publishOrderEvent(saved, "ORDER_WORKING");
            return saved;
        }

        BigDecimal remaining = sanitizeScale(order.getRemainingQuantity());
        if (remaining.compareTo(ZERO) > 0) {
            applyFill(order, currentPrice.get(), remaining);
        }
        updateOrderStatusAfterExecution(order);
        finalizeOrderReservation(order);
        order.setUpdatedAt(LocalDateTime.now());
        Order savedOrder = orderRepository.save(order);
        outboxEventService.publishOrderEvent(savedOrder, savedOrder.getStatus() == OrderStatus.FILLED
                ? "ORDER_FILLED"
                : "ORDER_PARTIALLY_FILLED");
        return savedOrder;
    }

    private boolean isExecutableLimitOrder(Order order, BigDecimal currentPrice) {
        BigDecimal limitPrice = sanitizeScale(order.getLimitPrice());
        if (order.getSide() == OrderSide.BUY) {
            return currentPrice.compareTo(limitPrice) <= 0;
        }
        return currentPrice.compareTo(limitPrice) >= 0;
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
        fill.setFillPrice(sanitizeScale(fillPrice));
        fill.setFilledAt(LocalDateTime.now());
        fillRepository.save(fill);

        BigDecimal currentFilled = sanitizeScale(order.getFilledQuantity());
        BigDecimal nextFilled = currentFilled.add(fillQuantity).setScale(6, RoundingMode.HALF_UP);
        BigDecimal requested = sanitizeScale(order.getQuantity());
        BigDecimal remaining = requested.subtract(nextFilled).max(BigDecimal.ZERO).setScale(6, RoundingMode.HALF_UP);
        order.setFilledQuantity(nextFilled);
        order.setRemainingQuantity(remaining);
        order.setLastExecutedAt(fill.getFilledAt());

        BigDecimal fillNotional = sanitizeScale(fillPrice.multiply(fillQuantity));
        if (order.getSide() == OrderSide.BUY) {
            accountService.settleBuyFill(order.getAccount(), order, fillNotional, "BUY fill settlement for order " + order.getId());
        } else {
            accountService.settleSellProceeds(order.getAccount(), order, fillNotional, "SELL fill proceeds for order " + order.getId());
        }

        upsertPosition(order, fillPrice, fillQuantity);
    }

    private void upsertPosition(Order order, BigDecimal fillPrice, BigDecimal fillQuantity) {
        Position position = positionRepository.findByStrategyRunIdAndInstrumentId(
                order.getStrategyRun().getId(),
                order.getInstrument().getId()
        ).orElseGet(() -> {
            Position newPosition = new Position();
            newPosition.setAccount(order.getAccount());
            newPosition.setStrategyRun(order.getStrategyRun());
            newPosition.setInstrument(order.getInstrument());
            newPosition.setNetQuantity(ZERO);
            newPosition.setAveragePrice(ZERO);
            newPosition.setUpdatedAt(LocalDateTime.now());
            return newPosition;
        });

        BigDecimal existingQuantity = sanitizeScale(position.getNetQuantity());

        if (order.getSide() == OrderSide.BUY) {
            BigDecimal newQuantity = existingQuantity.add(fillQuantity).setScale(6, RoundingMode.HALF_UP);
            BigDecimal weightedCost = existingQuantity.multiply(position.getAveragePrice())
                    .add(fillQuantity.multiply(fillPrice))
                    .setScale(6, RoundingMode.HALF_UP);
            BigDecimal newAveragePrice = ZERO;
            if (newQuantity.compareTo(ZERO) > 0) {
                newAveragePrice = weightedCost.divide(newQuantity, 6, RoundingMode.HALF_UP);
            }
            position.setNetQuantity(newQuantity);
            position.setAveragePrice(newAveragePrice);
        } else {
            BigDecimal newQuantity = existingQuantity.subtract(fillQuantity).setScale(6, RoundingMode.HALF_UP);
            position.setNetQuantity(newQuantity);
            if (newQuantity.compareTo(ZERO) <= 0) {
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

    private void finalizeOrderReservation(Order order) {
        if (order.getSide() == OrderSide.BUY && order.getReservedCashAmount().compareTo(ZERO) > 0
                && (order.getStatus() == OrderStatus.FILLED || order.getStatus() == OrderStatus.CANCELED || order.getStatus() == OrderStatus.EXPIRED)) {
            accountService.releaseReservedCash(
                    order.getAccount(),
                    order,
                    order.getReservedCashAmount(),
                    "Release remaining reservation for order " + order.getId()
            );
        }
    }

    private BigDecimal sanitizeScale(BigDecimal value) {
        if (value == null) {
            return ZERO;
        }
        return value.setScale(6, RoundingMode.HALF_UP);
    }
}
