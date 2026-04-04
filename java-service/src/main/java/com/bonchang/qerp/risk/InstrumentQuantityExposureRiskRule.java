package com.bonchang.qerp.risk;

import com.bonchang.qerp.order.Order;
import com.bonchang.qerp.order.OrderRepository;
import com.bonchang.qerp.order.OrderSide;
import com.bonchang.qerp.order.OrderStatus;
import java.math.BigDecimal;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class InstrumentQuantityExposureRiskRule implements RiskRule {

    private static final String RULE_NAME = "INSTRUMENT_QUANTITY_EXPOSURE";

    private final OrderRepository orderRepository;
    private final BigDecimal maxInstrumentQuantity;

    public InstrumentQuantityExposureRiskRule(
            OrderRepository orderRepository,
            @Value("${risk.max-instrument-quantity:2000}") BigDecimal maxInstrumentQuantity
    ) {
        this.orderRepository = orderRepository;
        this.maxInstrumentQuantity = maxInstrumentQuantity;
    }

    @Override
    public RiskRuleResult evaluate(Order order) {
        BigDecimal existingSignedExposure = orderRepository.sumSignedQuantityByInstrumentIdAndStatuses(
                order.getInstrument().getId(),
                List.of(OrderStatus.APPROVED, OrderStatus.PARTIALLY_FILLED, OrderStatus.FILLED)
        );
        BigDecimal orderExposure = order.getSide() == OrderSide.BUY
                ? order.getQuantity()
                : order.getQuantity().negate();
        BigDecimal projectedQuantity = existingSignedExposure.add(orderExposure);
        boolean passed = projectedQuantity.abs().compareTo(maxInstrumentQuantity) <= 0;
        if (passed) {
            return new RiskRuleResult(RULE_NAME, true, "Instrument quantity exposure within limit");
        }
        String message = "Projected instrument quantity exposure %s exceeds max instrument quantity %s"
                .formatted(projectedQuantity, maxInstrumentQuantity);
        return new RiskRuleResult(RULE_NAME, false, message);
    }
}
