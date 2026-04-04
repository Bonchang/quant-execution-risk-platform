package com.bonchang.qerp.appauth;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record AppMeResponse(
        Long userId,
        String authType,
        String role,
        String displayName,
        Account account,
        MarketConnection marketConnection
) {

    public record Account(
            Long accountId,
            String accountCode,
            String ownerName,
            String baseCurrency,
            BigDecimal availableCash,
            BigDecimal reservedCash
    ) {
    }

    public record MarketConnection(
            String status,
            String source,
            boolean stale,
            long staleQuoteCount,
            LocalDateTime lastQuoteReceivedAt
    ) {
    }
}
