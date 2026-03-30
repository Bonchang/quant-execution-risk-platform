package com.bonchang.qerp.market;

import java.time.LocalDate;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MarketPriceRepository extends JpaRepository<MarketPrice, Long> {

    Optional<MarketPrice> findFirstByInstrumentIdOrderByPriceDateDescIdDesc(Long instrumentId);

    Optional<MarketPrice> findByInstrumentIdAndPriceDate(Long instrumentId, LocalDate priceDate);
}
