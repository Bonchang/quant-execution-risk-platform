package com.bonchang.qerp.strategyrun.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateStrategyRunRequest(
        @NotNull Long accountId,
        @NotBlank @Size(max = 128) String strategyName,
        @NotBlank String parametersJson
) {
}
