package com.bonchang.qerp.risk;

import com.bonchang.qerp.account.CashBalanceRepository;
import com.bonchang.qerp.order.Order;
import com.bonchang.qerp.order.OrderSide;
import com.bonchang.qerp.order.OrderPricingService;
import java.math.BigDecimal;
import java.math.RoundingMode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AvailableCashRiskRule implements RiskRule {

    private static final String RULE_NAME = "AVAILABLE_CASH";
    private static final BigDecimal ZERO = BigDecimal.ZERO.setScale(6, RoundingMode.HALF_UP);

    private final CashBalanceRepository cashBalanceRepository;
    private final OrderPricingService orderPricingService;

    @Override
    public RiskRuleResult evaluate(Order order) {
        if (order.getSide() != OrderSide.BUY) {
            return new RiskRuleResult(RULE_NAME, true, "Available cash check not required for SELL order");
        }

        BigDecimal availableCash = cashBalanceRepository.findByAccountId(order.getAccount().getId())
                .map(balance -> balance.getAvailableCash().setScale(6, RoundingMode.HALF_UP))
                .orElse(ZERO);
        BigDecimal estimatedNotional = orderPricingService.calculateEstimatedBuyNotional(order);
        boolean passed = availableCash.compareTo(estimatedNotional) >= 0;
        if (passed) {
            return new RiskRuleResult(RULE_NAME, true, "Available cash covers estimated order notional");
        }
        String message = "Estimated buy notional %s exceeds available cash %s".formatted(estimatedNotional, availableCash);
        return new RiskRuleResult(RULE_NAME, false, message);
    }
}
