package com.bonchang.qerp.strategyrun;

import com.bonchang.qerp.account.Account;
import com.bonchang.qerp.account.AccountService;
import com.bonchang.qerp.strategyrun.dto.CreateStrategyRunRequest;
import com.bonchang.qerp.strategyrun.dto.StrategyRunResponse;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class StrategyRunService {

    private final StrategyRunRepository strategyRunRepository;
    private final AccountService accountService;

    @Transactional
    public StrategyRunResponse createStrategyRun(CreateStrategyRunRequest request) {
        Account account = accountService.getReferenceAccount(request.accountId());
        StrategyRun strategyRun = new StrategyRun();
        strategyRun.setAccount(account);
        strategyRun.setStrategyName(request.strategyName());
        strategyRun.setRunAt(LocalDateTime.now());
        strategyRun.setParametersJson(request.parametersJson());
        StrategyRun saved = strategyRunRepository.save(strategyRun);
        return toResponse(saved);
    }

    public List<StrategyRunResponse> listStrategyRuns(int limit) {
        return strategyRunRepository.findAllByOrderByRunAtDescIdDesc(PageRequest.of(0, Math.min(Math.max(limit, 1), 100)))
                .stream()
                .map(this::toResponse)
                .toList();
    }

    public StrategyRunResponse getStrategyRun(Long id) {
        return strategyRunRepository.findById(id)
                .map(this::toResponse)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "strategyRun not found"));
    }

    private StrategyRunResponse toResponse(StrategyRun strategyRun) {
        return new StrategyRunResponse(
                strategyRun.getId(),
                strategyRun.getAccount().getId(),
                strategyRun.getAccount().getAccountCode(),
                strategyRun.getStrategyName(),
                strategyRun.getRunAt(),
                strategyRun.getParametersJson()
        );
    }
}
