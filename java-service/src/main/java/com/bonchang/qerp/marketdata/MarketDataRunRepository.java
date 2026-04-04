package com.bonchang.qerp.marketdata;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MarketDataRunRepository extends JpaRepository<MarketDataRun, Long> {

    Optional<MarketDataRun> findFirstByOrderByFinishedAtDescIdDesc();
}
