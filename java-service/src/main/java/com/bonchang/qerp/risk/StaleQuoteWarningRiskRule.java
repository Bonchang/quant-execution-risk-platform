package com.bonchang.qerp.risk;

import com.bonchang.qerp.order.Order;
import com.bonchang.qerp.order.OrderPricingService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class StaleQuoteWarningRiskRule implements RiskRule {

    private static final String RULE_NAME = "QUOTE_FRESHNESS_WARNING";

    private final OrderPricingService orderPricingService;

    @Override
    public RiskRuleResult evaluate(Order order) {
        return orderPricingService.resolvePriceSnapshot(order.getInstrument().getId())
                .map(snapshot -> {
                    if (snapshot.quoteAvailable() && snapshot.stale()) {
                        return new RiskRuleResult(
                                RULE_NAME,
                                true,
                                "Latest quote is stale; order remains eligible but audit warning was recorded"
                        );
                    }
                    if (snapshot.quoteAvailable()) {
                        return new RiskRuleResult(RULE_NAME, true, "Latest quote freshness check passed");
                    }
                    return new RiskRuleResult(
                            RULE_NAME,
                            true,
                            "No live quote available; pricing falls back to latest close or default price"
                    );
                })
                .orElseGet(() -> new RiskRuleResult(
                        RULE_NAME,
                        true,
                        "No market reference found; pricing falls back to default execution price"
                ));
    }
}
