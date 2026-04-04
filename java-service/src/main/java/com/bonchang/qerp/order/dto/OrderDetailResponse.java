package com.bonchang.qerp.order.dto;

import com.bonchang.qerp.order.OrderSide;
import com.bonchang.qerp.order.OrderStatus;
import com.bonchang.qerp.order.OrderType;
import com.bonchang.qerp.order.TimeInForce;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record OrderDetailResponse(
        Long id,
        Long accountId,
        String accountCode,
        String ownerName,
        String baseCurrency,
        BigDecimal availableCash,
        BigDecimal reservedCash,
        Long strategyRunId,
        String strategyName,
        Long instrumentId,
        String instrumentSymbol,
        OrderSide side,
        BigDecimal quantity,
        BigDecimal limitPrice,
        BigDecimal reservedCashAmount,
        BigDecimal filledQuantity,
        BigDecimal remainingQuantity,
        OrderType orderType,
        TimeInForce timeInForce,
        OrderStatus status,
        String clientOrderId,
        LocalDateTime createdAt,
        LocalDateTime expiresAt,
        LocalDateTime lastExecutedAt,
        LocalDateTime updatedAt,
        List<FillItem> fills,
        List<RiskCheckItem> riskChecks,
        List<OutboxEventItem> outboxEvents,
        List<CashLedgerItem> cashLedgerEntries
) {

    public record FillItem(
            Long id,
            BigDecimal fillQuantity,
            BigDecimal fillPrice,
            LocalDateTime filledAt
    ) {
    }

    public record RiskCheckItem(
            Long id,
            String ruleName,
            boolean passed,
            String message,
            LocalDateTime checkedAt
    ) {
    }

    public record OutboxEventItem(
            Long id,
            String aggregateType,
            Long aggregateId,
            String eventType,
            String processingStatus,
            LocalDateTime createdAt,
            LocalDateTime processedAt
    ) {
    }

    public record CashLedgerItem(
            Long id,
            String entryType,
            BigDecimal amount,
            BigDecimal availableCashAfter,
            BigDecimal reservedCashAfter,
            String note,
            LocalDateTime createdAt
    ) {
    }
}
