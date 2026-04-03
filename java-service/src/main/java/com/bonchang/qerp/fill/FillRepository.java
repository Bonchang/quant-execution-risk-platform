package com.bonchang.qerp.fill;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface FillRepository extends JpaRepository<Fill, Long> {

    @Query("""
            SELECT f
            FROM Fill f
            JOIN FETCH f.order o
            WHERE f.strategyRun.id = :strategyRunId
            ORDER BY f.filledAt ASC, f.id ASC
            """)
    List<Fill> findByStrategyRunIdOrderByFilledAtAscIdAsc(@Param("strategyRunId") Long strategyRunId);
}
