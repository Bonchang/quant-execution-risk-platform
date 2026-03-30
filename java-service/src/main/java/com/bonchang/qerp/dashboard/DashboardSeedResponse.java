package com.bonchang.qerp.dashboard;

public record DashboardSeedResponse(
        Long strategyRunId,
        Long instrumentId,
        String instrumentSymbol,
        String message
) {
}
