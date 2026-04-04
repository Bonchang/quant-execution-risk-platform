package com.bonchang.qerp.account;

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
@Table(
        name = "cash_balance",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_cash_balance_account", columnNames = "account_id")
        }
)
@Getter
@Setter
@NoArgsConstructor
public class CashBalance {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "account_id", nullable = false)
    private Account account;

    @Column(name = "available_cash", nullable = false, precision = 19, scale = 6)
    private BigDecimal availableCash;

    @Column(name = "reserved_cash", nullable = false, precision = 19, scale = 6)
    private BigDecimal reservedCash;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}
