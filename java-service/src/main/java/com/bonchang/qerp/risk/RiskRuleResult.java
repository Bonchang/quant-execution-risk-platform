package com.bonchang.qerp.risk;

public record RiskRuleResult(
        String ruleName,
        boolean passed,
        String message
) {
}
