package com.bonchang.qerp.appview;

import com.bonchang.qerp.appauth.AppSessionService;
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
    private final AppSessionService appSessionService;

    @GetMapping("/home")
    public HomeScreenResponse home() {
        Long accountId = appSessionService.currentPrincipal().map(principal -> principal.accountId()).orElse(null);
        return appExperienceService.loadHome(accountId);
    }

    @GetMapping("/discover")
    public DiscoverScreenResponse discover() {
        return appExperienceService.loadDiscover();
    }

    @GetMapping("/stocks/{symbol}")
    public StockDetailResponse stock(@PathVariable String symbol) {
        Long accountId = appSessionService.currentPrincipal().map(principal -> principal.accountId()).orElse(null);
        return appExperienceService.loadStock(symbol, accountId);
    }

    @GetMapping("/portfolio")
    public PortfolioScreenResponse portfolio() {
        return appExperienceService.loadPortfolio(appSessionService.requirePrincipalWithAccount().accountId());
    }

    @GetMapping("/orders")
    public OrdersScreenResponse orders() {
        return appExperienceService.loadOrders(appSessionService.requirePrincipalWithAccount().accountId());
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
