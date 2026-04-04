package com.bonchang.qerp.research;

import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/research")
@RequiredArgsConstructor
public class ResearchController {

    private final ResearchArtifactService researchArtifactService;

    @GetMapping("/runs")
    public List<ResearchRunSummaryResponse> listRuns() {
        return researchArtifactService.listRuns();
    }

    @GetMapping("/runs/{runId}")
    public ResearchRunDetailResponse getRun(@PathVariable String runId) {
        return researchArtifactService.getRun(runId);
    }
}
