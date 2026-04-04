package com.bonchang.qerp.order;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface OrderRepository extends JpaRepository<Order, Long> {

    @Query("""
            select coalesce(sum(
                case
                    when o.side = com.bonchang.qerp.order.OrderSide.BUY then o.quantity
                    else -o.quantity
                end
            ), 0)
            from Order o
            where o.account.id = :accountId
              and o.instrument.id = :instrumentId
              and o.status in :statuses
            """)
    BigDecimal sumSignedQuantityByAccountIdAndInstrumentIdAndStatuses(
            @Param("accountId") Long accountId,
            @Param("instrumentId") Long instrumentId,
            @Param("statuses") Collection<OrderStatus> statuses
    );

    List<Order> findAllByOrderByIdDesc(Pageable pageable);

    Optional<Order> findById(Long id);

    List<Order> findByStatusAndTimeInForceAndExpiresAtBefore(
            OrderStatus status,
            TimeInForce timeInForce,
            java.time.LocalDateTime expiresAt
    );
}
