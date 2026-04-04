package com.bonchang.qerp.strategyrun.dto;

import java.time.LocalDateTime;

public record StrategyRunResponse(
        Long id,
        Long accountId,
        String accountCode,
        String strategyName,
        LocalDateTime runAt,
        String parametersJson
) {
}
