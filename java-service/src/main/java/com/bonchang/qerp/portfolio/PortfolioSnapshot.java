package com.bonchang.qerp.portfolio;

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
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "portfolio_snapshot")
@Getter
@Setter
@NoArgsConstructor
public class PortfolioSnapshot {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "strategy_run_id", nullable = false)
    private StrategyRun strategyRun;

    @Column(name = "snapshot_at", nullable = false)
    private LocalDateTime snapshotAt;

    @Column(name = "total_market_value", nullable = false, precision = 19, scale = 6)
    private BigDecimal totalMarketValue;

    @Column(name = "unrealized_pnl", nullable = false, precision = 19, scale = 6)
    private BigDecimal unrealizedPnl;

    @Column(name = "realized_pnl", nullable = false, precision = 19, scale = 6)
    private BigDecimal realizedPnl;

    @Column(name = "total_pnl", nullable = false, precision = 19, scale = 6)
    private BigDecimal totalPnl;

    @Column(name = "return_rate", nullable = false, precision = 19, scale = 6)
    private BigDecimal returnRate;
}
