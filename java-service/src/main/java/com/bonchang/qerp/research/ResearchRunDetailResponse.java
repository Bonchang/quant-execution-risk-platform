package com.bonchang.qerp.research;

import java.util.Map;
import java.util.List;

public record ResearchRunDetailResponse(
        String runId,
        String strategyName,
        String instrumentSymbol,
        String generatedAt,
        Map<String, Object> metrics,
        Map<String, Object> config,
        Map<String, String> artifactFiles,
        Map<String, Boolean> artifactAvailability,
        List<Map<String, Object>> equityCurveRows,
        List<Map<String, Object>> tradeRows,
        List<Map<String, Object>> signalRows,
        String reportPath
) {
}
