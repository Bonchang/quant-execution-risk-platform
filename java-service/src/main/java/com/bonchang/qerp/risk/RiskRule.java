package com.bonchang.qerp.risk;

import com.bonchang.qerp.order.Order;

public interface RiskRule {

    RiskRuleResult evaluate(Order order);
}
