package com.bonchang.qerp.portfolio;

import com.bonchang.qerp.fill.Fill;
import com.bonchang.qerp.order.OrderSide;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class RealizedPnlCalculator {

    private static final BigDecimal ZERO = BigDecimal.ZERO.setScale(6, RoundingMode.HALF_UP);

    /**
     * MVP 실현손익 규칙:
     * - BUY fill: 실현손익 없음, 평균단가/수량만 갱신
     * - SELL fill: qty * (sellPrice - avgCostBeforeSell)를 누적
     * - 단, SELL qty 중 기존 롱 재고를 초과하는 부분은 실현손익으로 과대계상하지 않도록 제외
     */
    public BigDecimal calculate(List<Fill> fills) {
        Map<Long, PositionCostState> states = new HashMap<>();
        BigDecimal realizedPnl = ZERO;

        for (Fill fill : fills) {
            Long instrumentId = fill.getInstrument().getId();
            PositionCostState state = states.computeIfAbsent(instrumentId, id -> new PositionCostState());
            BigDecimal quantity = toScale(fill.getFillQuantity());
            BigDecimal price = toScale(fill.getFillPrice());

            if (fill.getOrder().getSide() == OrderSide.BUY) {
                BigDecimal newQuantity = state.netQuantity.add(quantity).setScale(6, RoundingMode.HALF_UP);
                BigDecimal weightedCost = state.netQuantity.multiply(state.averagePrice)
                        .add(quantity.multiply(price))
                        .setScale(6, RoundingMode.HALF_UP);
                state.netQuantity = newQuantity;
                state.averagePrice = newQuantity.compareTo(BigDecimal.ZERO) > 0
                        ? weightedCost.divide(newQuantity, 6, RoundingMode.HALF_UP)
                        : ZERO;
                continue;
            }

            BigDecimal averageBeforeSell = state.averagePrice;
            BigDecimal matchedQuantity = quantity.min(state.netQuantity.max(BigDecimal.ZERO)).setScale(6, RoundingMode.HALF_UP);
            BigDecimal contribution = matchedQuantity.multiply(price.subtract(averageBeforeSell))
                    .setScale(6, RoundingMode.HALF_UP);
            realizedPnl = realizedPnl.add(contribution).setScale(6, RoundingMode.HALF_UP);

            BigDecimal newQuantity = state.netQuantity.subtract(quantity).setScale(6, RoundingMode.HALF_UP);
            state.netQuantity = newQuantity;
            if (newQuantity.compareTo(BigDecimal.ZERO) <= 0) {
                state.averagePrice = ZERO;
            }
        }

        return realizedPnl;
    }

    private BigDecimal toScale(BigDecimal value) {
        if (value == null) {
            return ZERO;
        }
        return value.setScale(6, RoundingMode.HALF_UP);
    }

    private static class PositionCostState {
        private BigDecimal netQuantity = BigDecimal.ZERO.setScale(6, RoundingMode.HALF_UP);
        private BigDecimal averagePrice = BigDecimal.ZERO.setScale(6, RoundingMode.HALF_UP);
    }
}
