package com.bonchang.qerp.strategyrun;

import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StrategyRunRepository extends JpaRepository<StrategyRun, Long> {

    List<StrategyRun> findAllByOrderByRunAtDescIdDesc(Pageable pageable);
}
