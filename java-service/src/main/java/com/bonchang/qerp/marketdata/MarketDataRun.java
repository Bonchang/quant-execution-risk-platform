package com.bonchang.qerp.marketdata;

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
@Table(name = "market_data_run")
@Getter
@Setter
@NoArgsConstructor
public class MarketDataRun {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "started_at", nullable = false)
    private LocalDateTime startedAt;

    @Column(name = "finished_at", nullable = false)
    private LocalDateTime finishedAt;

    @Column(name = "source", nullable = false, length = 32)
    private String source;

    @Column(name = "total_instruments", nullable = false)
    private int totalInstruments;

    @Column(name = "success_count", nullable = false)
    private int successCount;

    @Column(name = "failure_count", nullable = false)
    private int failureCount;

    @Column(name = "run_status", nullable = false, length = 16)
    private String runStatus;

    @Column(name = "last_quote_received_at")
    private LocalDateTime lastQuoteReceivedAt;

    @Column(name = "updated_symbols_json", nullable = false, columnDefinition = "TEXT")
    private String updatedSymbolsJson;

    @Column(name = "failure_messages_json", nullable = false, columnDefinition = "TEXT")
    private String failureMessagesJson;

    @Column(name = "stale_instruments_json", nullable = false, columnDefinition = "TEXT")
    private String staleInstrumentsJson;
}
