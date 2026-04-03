package com.bonchang.qerp.position;

import java.util.Optional;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PositionRepository extends JpaRepository<Position, Long> {

    Optional<Position> findByStrategyRunIdAndInstrumentId(Long strategyRunId, Long instrumentId);

    List<Position> findByStrategyRunId(Long strategyRunId);
}
