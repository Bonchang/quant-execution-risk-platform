package com.bonchang.qerp.strategyrun;

import com.bonchang.qerp.strategyrun.dto.CreateStrategyRunRequest;
import com.bonchang.qerp.strategyrun.dto.StrategyRunResponse;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/strategy-runs")
@RequiredArgsConstructor
public class StrategyRunController {

    private final StrategyRunService strategyRunService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public StrategyRunResponse create(@Valid @RequestBody CreateStrategyRunRequest request) {
        return strategyRunService.createStrategyRun(request);
    }

    @GetMapping
    public List<StrategyRunResponse> list(@RequestParam(defaultValue = "20") int limit) {
        return strategyRunService.listStrategyRuns(limit);
    }

    @GetMapping("/{id}")
    public StrategyRunResponse detail(@PathVariable Long id) {
        return strategyRunService.getStrategyRun(id);
    }
}
