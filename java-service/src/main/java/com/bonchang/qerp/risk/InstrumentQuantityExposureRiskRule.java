package com.bonchang.qerp.risk;

import com.bonchang.qerp.order.Order;
import com.bonchang.qerp.order.OrderRepository;
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
        BigDecimal existingApprovedQuantity = orderRepository.sumQuantityByInstrumentIdAndStatuses(
                order.getInstrument().getId(),
                List.of(OrderStatus.APPROVED, OrderStatus.FILLED)
        );
        BigDecimal projectedQuantity = existingApprovedQuantity.add(order.getQuantity());
        boolean passed = projectedQuantity.compareTo(maxInstrumentQuantity) <= 0;
        if (passed) {
            return new RiskRuleResult(RULE_NAME, true, "Instrument quantity exposure within limit");
        }
        String message = "Projected instrument quantity %s exceeds max instrument quantity %s"
                .formatted(projectedQuantity, maxInstrumentQuantity);
        return new RiskRuleResult(RULE_NAME, false, message);
    }
}
