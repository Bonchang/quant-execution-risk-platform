package com.bonchang.qerp.dashboard;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record DashboardOptionsResponse(
        List<StrategyRunOption> strategyRuns,
        List<InstrumentOption> instruments
) {

    public record StrategyRunOption(
            Long id,
            String strategyName,
            String runAt
    ) {
    }

    public record InstrumentOption(
            Long id,
            String symbol,
            String name,
            String market,
            BigDecimal latestClosePrice,
            LocalDate latestPriceDate
    ) {
    }
}
