package com.bonchang.qerp.position;

import java.util.List;
import java.util.Optional;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PositionRepository extends JpaRepository<Position, Long> {

    Optional<Position> findByStrategyRunIdAndInstrumentId(Long strategyRunId, Long instrumentId);

    List<Position> findByStrategyRunId(Long strategyRunId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select p
            from Position p
            where p.strategyRun.id = :strategyRunId
              and p.instrument.id = :instrumentId
            """)
    Optional<Position> lockByStrategyRunIdAndInstrumentId(
            @Param("strategyRunId") Long strategyRunId,
            @Param("instrumentId") Long instrumentId
    );

    @Query("""
            select distinct p.strategyRun.id
            from Position p
            where p.instrument.id = :instrumentId
              and p.netQuantity <> 0
            """)
    List<Long> findDistinctStrategyRunIdsByInstrumentId(@Param("instrumentId") Long instrumentId);
}
