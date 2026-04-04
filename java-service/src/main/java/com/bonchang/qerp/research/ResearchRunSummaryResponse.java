package com.bonchang.qerp.research;

import java.util.Map;

public record ResearchRunSummaryResponse(
        String runId,
        String strategyName,
        String instrumentSymbol,
        String generatedAt,
        Map<String, Object> metrics,
        Map<String, Boolean> artifactAvailability,
        String reportPath
) {
}
