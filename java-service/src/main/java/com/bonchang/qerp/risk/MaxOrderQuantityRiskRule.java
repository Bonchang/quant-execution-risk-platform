package com.bonchang.qerp.risk;

import com.bonchang.qerp.order.Order;
import java.math.BigDecimal;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class MaxOrderQuantityRiskRule implements RiskRule {

    private static final String RULE_NAME = "MAX_ORDER_QUANTITY";

    private final BigDecimal maxOrderQuantity;

    public MaxOrderQuantityRiskRule(@Value("${risk.max-order-quantity:1000}") BigDecimal maxOrderQuantity) {
        this.maxOrderQuantity = maxOrderQuantity;
    }

    @Override
    public RiskRuleResult evaluate(Order order) {
        BigDecimal quantity = order.getQuantity();
        boolean passed = quantity.compareTo(maxOrderQuantity) <= 0;
        if (passed) {
            return new RiskRuleResult(RULE_NAME, true, "Quantity within max order quantity limit");
        }
        String message = "Quantity %s exceeds max order quantity %s".formatted(quantity, maxOrderQuantity);
        return new RiskRuleResult(RULE_NAME, false, message);
    }
}
