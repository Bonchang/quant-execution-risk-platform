package com.bonchang.qerp.execution;

import com.bonchang.qerp.fill.Fill;
import com.bonchang.qerp.fill.FillRepository;
import com.bonchang.qerp.market.MarketPrice;
import com.bonchang.qerp.market.MarketPriceRepository;
import com.bonchang.qerp.order.Order;
import com.bonchang.qerp.order.OrderRepository;
import com.bonchang.qerp.order.OrderSide;
import com.bonchang.qerp.order.OrderStatus;
import com.bonchang.qerp.position.Position;
import com.bonchang.qerp.position.PositionRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class OrderExecutionService {

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

        Fill fill = new Fill();
        fill.setOrder(order);
        fill.setStrategyRun(order.getStrategyRun());
        fill.setInstrument(order.getInstrument());
        fill.setFillQuantity(order.getQuantity());
        fill.setFillPrice(fillPrice);
        fill.setFilledAt(LocalDateTime.now());
        fillRepository.save(fill);

        upsertPosition(order, fillPrice);

        order.setStatus(OrderStatus.FILLED);
        return orderRepository.save(order);
    }

    private BigDecimal resolveFillPrice(Order order) {
        Optional<MarketPrice> latest = marketPriceRepository.findFirstByInstrumentIdOrderByPriceDateDescIdDesc(
                order.getInstrument().getId()
        );
        return latest.map(MarketPrice::getClosePrice).orElse(defaultFillPrice);
    }

    private void upsertPosition(Order order, BigDecimal fillPrice) {
        Position position = positionRepository.findByStrategyRunIdAndInstrumentId(
                order.getStrategyRun().getId(),
                order.getInstrument().getId()
        ).orElseGet(() -> {
            Position newPosition = new Position();
            newPosition.setStrategyRun(order.getStrategyRun());
            newPosition.setInstrument(order.getInstrument());
            newPosition.setNetQuantity(BigDecimal.ZERO.setScale(6, RoundingMode.HALF_UP));
            newPosition.setAveragePrice(BigDecimal.ZERO.setScale(6, RoundingMode.HALF_UP));
            newPosition.setUpdatedAt(LocalDateTime.now());
            return newPosition;
        });

        BigDecimal existingQuantity = position.getNetQuantity();
        BigDecimal fillQuantity = order.getQuantity();

        if (order.getSide() == OrderSide.BUY) {
            BigDecimal newQuantity = existingQuantity.add(fillQuantity);
            BigDecimal weightedCost = existingQuantity.multiply(position.getAveragePrice())
                    .add(fillQuantity.multiply(fillPrice));
            BigDecimal newAveragePrice = BigDecimal.ZERO.setScale(6, RoundingMode.HALF_UP);
            if (newQuantity.compareTo(BigDecimal.ZERO) > 0) {
                newAveragePrice = weightedCost.divide(newQuantity, 6, RoundingMode.HALF_UP);
            }
            position.setNetQuantity(newQuantity.setScale(6, RoundingMode.HALF_UP));
            position.setAveragePrice(newAveragePrice);
        } else {
            BigDecimal newQuantity = existingQuantity.subtract(fillQuantity);
            position.setNetQuantity(newQuantity.setScale(6, RoundingMode.HALF_UP));
            if (newQuantity.compareTo(BigDecimal.ZERO) <= 0) {
                position.setAveragePrice(BigDecimal.ZERO.setScale(6, RoundingMode.HALF_UP));
            }
        }

        position.setUpdatedAt(LocalDateTime.now());
        positionRepository.save(position);
    }
}
