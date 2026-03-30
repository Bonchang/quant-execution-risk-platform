package com.bonchang.qerp.risk;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "risk_check_result")
@Getter
@Setter
@NoArgsConstructor
public class RiskCheckResult {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "order_id", nullable = false)
    private Long orderId;

    @Column(name = "rule_name", nullable = false, length = 64)
    private String ruleName;

    @Column(name = "passed", nullable = false)
    private boolean passed;

    @Column(name = "message", nullable = false, length = 255)
    private String message;

    @Column(name = "checked_at", nullable = false)
    private LocalDateTime checkedAt;
}
