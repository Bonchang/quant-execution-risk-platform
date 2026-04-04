package com.bonchang.qerp.research;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class ResearchArtifactService {

    private final ObjectMapper objectMapper;

    @Value("${research.artifacts-path:../python-research/artifacts}")
    private String artifactsPath;

    public List<ResearchRunSummaryResponse> listRuns() {
        if (!Files.exists(basePath())) {
            return List.of();
        }
        try {
            return Files.list(basePath())
                    .filter(Files::isDirectory)
                    .sorted(Comparator.comparing(Path::getFileName).reversed())
                    .map(this::toSummary)
                    .toList();
        } catch (IOException ex) {
            throw new IllegalStateException("failed to read research artifacts", ex);
        }
    }

    public ResearchRunDetailResponse getRun(String runId) {
        Path reportPath = basePath().resolve(runId).resolve("report.json");
        if (!Files.exists(reportPath)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "research run not found");
        }
        return toDetail(reportPath.getParent());
    }

    public ResearchRunSummaryResponse latestRun() {
        return listRuns().stream().findFirst().orElse(null);
    }

    private ResearchRunSummaryResponse toSummary(Path runDir) {
        Map<String, Object> report = readReport(runDir);
        return new ResearchRunSummaryResponse(
                String.valueOf(report.getOrDefault("runId", runDir.getFileName().toString())),
                String.valueOf(report.getOrDefault("strategyName", "unknown")),
                String.valueOf(report.getOrDefault("instrumentSymbol", "-")),
                String.valueOf(report.getOrDefault("generatedAt", "")),
                castMap(report.get("metrics")),
                runDir.resolve("report.json").toString()
        );
    }

    private ResearchRunDetailResponse toDetail(Path runDir) {
        Map<String, Object> report = readReport(runDir);
        return new ResearchRunDetailResponse(
                String.valueOf(report.getOrDefault("runId", runDir.getFileName().toString())),
                String.valueOf(report.getOrDefault("strategyName", "unknown")),
                String.valueOf(report.getOrDefault("instrumentSymbol", "-")),
                String.valueOf(report.getOrDefault("generatedAt", "")),
                castMap(report.get("metrics")),
                castMap(report.get("config")),
                castStringMap(report.get("artifactFiles")),
                runDir.resolve("report.json").toString()
        );
    }

    private Map<String, Object> readReport(Path runDir) {
        Path reportPath = runDir.resolve("report.json");
        try {
            return objectMapper.readValue(reportPath.toFile(), new TypeReference<>() {
            });
        } catch (IOException ex) {
            throw new IllegalStateException("failed to read report.json: " + reportPath, ex);
        }
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> castMap(Object value) {
        if (value instanceof Map<?, ?> map) {
            return (Map<String, Object>) map;
        }
        return Map.of();
    }

    @SuppressWarnings("unchecked")
    private Map<String, String> castStringMap(Object value) {
        if (value instanceof Map<?, ?> map) {
            return ((Map<String, Object>) map).entrySet().stream()
                    .collect(java.util.stream.Collectors.toMap(Map.Entry::getKey, entry -> String.valueOf(entry.getValue())));
        }
        return Map.of();
    }

    private Path basePath() {
        return Path.of(artifactsPath);
    }
}
