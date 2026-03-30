package com.bonchang.qerp.position;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PositionRepository extends JpaRepository<Position, Long> {

    Optional<Position> findByStrategyRunIdAndInstrumentId(Long strategyRunId, Long instrumentId);
}
