package com.bonchang.qerp.dashboard;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
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
}
