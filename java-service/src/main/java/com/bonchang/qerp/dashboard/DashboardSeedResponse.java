package com.bonchang.qerp.dashboard;

public record DashboardSeedResponse(
        Long accountId,
        String accountCode,
        Long strategyRunId,
        Long instrumentId,
        String instrumentSymbol,
        String message
) {
}
