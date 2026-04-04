package com.bonchang.qerp.position;

import com.bonchang.qerp.account.Account;
import com.bonchang.qerp.instrument.Instrument;
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
@Table(name = "position", uniqueConstraints = {
        @UniqueConstraint(name = "uk_position_strategy_run_instrument", columnNames = {"strategy_run_id", "instrument_id"})
})
@Getter
@Setter
@NoArgsConstructor
public class Position {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "strategy_run_id", nullable = false)
    private StrategyRun strategyRun;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "account_id", nullable = false)
    private Account account;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "instrument_id", nullable = false)
    private Instrument instrument;

    @Column(name = "net_quantity", nullable = false, precision = 19, scale = 6)
    private BigDecimal netQuantity;

    @Column(name = "average_price", nullable = false, precision = 19, scale = 6)
    private BigDecimal averagePrice;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}
