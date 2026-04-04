package com.bonchang.qerp.appview;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/app")
@RequiredArgsConstructor
public class AppExperienceController {

    private final AppExperienceService appExperienceService;

    @GetMapping("/home")
    public HomeScreenResponse home() {
        return appExperienceService.loadHome();
    }

    @GetMapping("/discover")
    public DiscoverScreenResponse discover() {
        return appExperienceService.loadDiscover();
    }

    @GetMapping("/stocks/{symbol}")
    public StockDetailResponse stock(@PathVariable String symbol) {
        return appExperienceService.loadStock(symbol);
    }

    @GetMapping("/portfolio")
    public PortfolioScreenResponse portfolio() {
        return appExperienceService.loadPortfolio();
    }

    @GetMapping("/orders")
    public OrdersScreenResponse orders() {
        return appExperienceService.loadOrders();
    }

    @GetMapping("/quant/overview")
    public QuantOverviewResponse quantOverview() {
        return appExperienceService.loadQuantOverview();
    }

    @GetMapping("/quant/strategies/{runId}")
    public QuantStrategyDetailResponse quantStrategy(@PathVariable String runId) {
        return appExperienceService.loadQuantStrategy(runId);
    }
}
