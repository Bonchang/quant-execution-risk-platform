package com.bonchang.qerp.risk;

import com.bonchang.qerp.order.Order;
import com.bonchang.qerp.order.OrderRepository;
import com.bonchang.qerp.order.OrderStatus;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class RiskEvaluationService {

    private final List<RiskRule> riskRules;
    private final RiskCheckResultRepository riskCheckResultRepository;
    private final OrderRepository orderRepository;

    public Order evaluateAndUpdateOrderStatus(Order order) {
        boolean allPassed = true;

        for (RiskRule rule : riskRules) {
            RiskRuleResult result = rule.evaluate(order);

            RiskCheckResult checkResult = new RiskCheckResult();
            checkResult.setOrderId(order.getId());
            checkResult.setRuleName(result.ruleName());
            checkResult.setPassed(result.passed());
            checkResult.setMessage(result.message());
            checkResult.setCheckedAt(LocalDateTime.now());
            riskCheckResultRepository.save(checkResult);

            if (!result.passed()) {
                allPassed = false;
            }
        }

        order.setStatus(allPassed ? OrderStatus.APPROVED : OrderStatus.REJECTED);
        return orderRepository.save(order);
    }
}
