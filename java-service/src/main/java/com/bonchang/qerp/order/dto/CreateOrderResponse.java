package com.bonchang.qerp.order.dto;

import com.bonchang.qerp.order.OrderStatus;
import com.bonchang.qerp.order.OrderType;
import com.bonchang.qerp.order.OrderSide;
import com.bonchang.qerp.order.TimeInForce;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public record CreateOrderResponse(
        Long id,
        Long accountId,
        Long strategyRunId,
        Long instrumentId,
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
        LocalDateTime updatedAt
) {
}
