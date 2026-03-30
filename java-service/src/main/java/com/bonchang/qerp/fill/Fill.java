package com.bonchang.qerp.fill;

import com.bonchang.qerp.instrument.Instrument;
import com.bonchang.qerp.order.Order;
import com.bonchang.qerp.strategyrun.StrategyRun;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "fill", uniqueConstraints = {
        @UniqueConstraint(name = "uk_fill_order_id", columnNames = {"order_id"})
})
@Getter
@Setter
@NoArgsConstructor
public class Fill {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "strategy_run_id", nullable = false)
    private StrategyRun strategyRun;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "instrument_id", nullable = false)
    private Instrument instrument;

    @Column(name = "fill_quantity", nullable = false, precision = 19, scale = 6)
    private BigDecimal fillQuantity;

    @Column(name = "fill_price", nullable = false, precision = 19, scale = 6)
    private BigDecimal fillPrice;

    @Column(name = "filled_at", nullable = false)
    private LocalDateTime filledAt;
}
