package com.bonchang.qerp.portfolio;

import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PortfolioSnapshotRepository extends JpaRepository<PortfolioSnapshot, Long> {

    Optional<PortfolioSnapshot> findFirstByOrderBySnapshotAtDescIdDesc();

    List<PortfolioSnapshot> findAllByOrderBySnapshotAtDescIdDesc(Pageable pageable);

    Optional<PortfolioSnapshot> findFirstByStrategyRunIdOrderBySnapshotAtDescIdDesc(Long strategyRunId);
}
