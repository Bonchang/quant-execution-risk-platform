package com.bonchang.qerp.risk;

import com.bonchang.qerp.order.Order;
import com.bonchang.qerp.order.OrderSide;
import com.bonchang.qerp.position.PositionRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import org.springframework.stereotype.Component;

@Component
public class AvailableLongPositionRiskRule implements RiskRule {

    private static final String RULE_NAME = "AVAILABLE_LONG_POSITION";
    private static final BigDecimal ZERO = BigDecimal.ZERO.setScale(6, RoundingMode.HALF_UP);

    private final PositionRepository positionRepository;

    public AvailableLongPositionRiskRule(PositionRepository positionRepository) {
        this.positionRepository = positionRepository;
    }

    @Override
    public RiskRuleResult evaluate(Order order) {
        if (order.getSide() != OrderSide.SELL) {
            return new RiskRuleResult(RULE_NAME, true, "Available long position check not required for BUY order");
        }

        BigDecimal availableQuantity = positionRepository.findByStrategyRunIdAndInstrumentId(
                        order.getStrategyRun().getId(),
                        order.getInstrument().getId()
                )
                .map(position -> position.getNetQuantity().max(BigDecimal.ZERO).setScale(6, RoundingMode.HALF_UP))
                .orElse(ZERO);
        BigDecimal requestedQuantity = order.getQuantity().setScale(6, RoundingMode.HALF_UP);
        boolean passed = requestedQuantity.compareTo(availableQuantity) <= 0;
        if (passed) {
            return new RiskRuleResult(RULE_NAME, true, "SELL quantity is covered by available long position");
        }

        String message = "SELL quantity %s exceeds available long position %s; short selling is not supported"
                .formatted(requestedQuantity, availableQuantity);
        return new RiskRuleResult(RULE_NAME, false, message);
    }
}
