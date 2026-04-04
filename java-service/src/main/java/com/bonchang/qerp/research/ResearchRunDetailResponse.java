package com.bonchang.qerp.research;

import java.util.Map;

public record ResearchRunDetailResponse(
        String runId,
        String strategyName,
        String instrumentSymbol,
        String generatedAt,
        Map<String, Object> metrics,
        Map<String, Object> config,
        Map<String, String> artifactFiles,
        String reportPath
) {
}
