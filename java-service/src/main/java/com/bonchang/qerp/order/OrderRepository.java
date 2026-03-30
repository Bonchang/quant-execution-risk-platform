package com.bonchang.qerp.order;

import java.math.BigDecimal;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface OrderRepository extends JpaRepository<Order, Long> {

    @Query("select coalesce(sum(o.quantity), 0) from Order o where o.instrument.id = :instrumentId and o.status = :status")
    BigDecimal sumQuantityByInstrumentIdAndStatus(
            @Param("instrumentId") Long instrumentId,
            @Param("status") OrderStatus status
    );
}
