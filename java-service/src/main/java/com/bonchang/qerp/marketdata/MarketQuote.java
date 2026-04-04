package com.bonchang.qerp.marketdata;

import com.bonchang.qerp.instrument.Instrument;
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
@Table(name = "market_quote")
@Getter
@Setter
@NoArgsConstructor
public class MarketQuote {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "instrument_id", nullable = false)
    private Instrument instrument;

    @Column(name = "quote_time", nullable = false)
    private LocalDateTime quoteTime;

    @Column(name = "last_price", nullable = false, precision = 19, scale = 6)
    private BigDecimal lastPrice;

    @Column(name = "bid_price", nullable = false, precision = 19, scale = 6)
    private BigDecimal bidPrice;

    @Column(name = "ask_price", nullable = false, precision = 19, scale = 6)
    private BigDecimal askPrice;

    @Column(name = "change_percent", nullable = false, precision = 19, scale = 6)
    private BigDecimal changePercent;

    @Column(name = "source", nullable = false, length = 32)
    private String source;

    @Column(name = "received_at", nullable = false)
    private LocalDateTime receivedAt;
}
