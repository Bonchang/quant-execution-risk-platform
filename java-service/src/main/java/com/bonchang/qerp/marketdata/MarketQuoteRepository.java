package com.bonchang.qerp.marketdata;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface MarketQuoteRepository extends JpaRepository<MarketQuote, Long> {

    Optional<MarketQuote> findByInstrumentId(Long instrumentId);

    Optional<MarketQuote> findFirstByOrderByReceivedAtDescIdDesc();

    List<MarketQuote> findAllByOrderByReceivedAtDescIdDesc(Pageable pageable);

    long countByReceivedAtBefore(LocalDateTime threshold);

    @Query("""
            select mq
            from MarketQuote mq
            join fetch mq.instrument i
            where mq.receivedAt < :threshold
            order by mq.receivedAt asc, i.symbol asc
            """)
    List<MarketQuote> findStaleQuotes(@Param("threshold") LocalDateTime threshold);

    @Query("""
            select mq
            from MarketQuote mq
            join fetch mq.instrument i
            where i.symbol = :symbol
            """)
    Optional<MarketQuote> findByInstrumentSymbol(@Param("symbol") String symbol);
}
