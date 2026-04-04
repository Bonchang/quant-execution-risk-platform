package com.bonchang.qerp.dashboard;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final DashboardService dashboardService;

    @GetMapping("/overview")
    public DashboardOverviewResponse getOverview(@RequestParam(defaultValue = "20") int limit) {
        int normalized = Math.min(Math.max(limit, 1), 100);
        return dashboardService.loadOverview(normalized);
    }

    @GetMapping("/timeline")
    public DashboardTimelineResponse getTimeline(@RequestParam(defaultValue = "50") int limit) {
        int normalized = Math.min(Math.max(limit, 1), 200);
        return dashboardService.loadTimeline(normalized);
    }

    @GetMapping("/options")
    public DashboardOptionsResponse getOptions() {
        return dashboardService.loadOptions();
    }

    @PostMapping("/seed-demo")
    @ResponseStatus(HttpStatus.CREATED)
    public DashboardSeedResponse seedDemo() {
        return dashboardService.seedDemoData();
    }

    @PostMapping("/portfolio-snapshots/refresh")
    public DashboardPortfolioRefreshResponse refreshPortfolioSnapshots() {
        return dashboardService.refreshPortfolioSnapshots();
    }
}
