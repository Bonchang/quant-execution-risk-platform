package com.bonchang.qerp.account;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record AccountSummaryResponse(
        Long id,
        String accountCode,
        String ownerName,
        String baseCurrency,
        BigDecimal availableCash,
        BigDecimal reservedCash,
        LocalDateTime updatedAt
) {
}
