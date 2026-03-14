package com.bonchang.qerp.order.dto;

import com.bonchang.qerp.order.OrderStatus;
import com.bonchang.qerp.order.OrderType;
import com.bonchang.qerp.order.OrderSide;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public record CreateOrderResponse(
        Long id,
        Long strategyRunId,
        Long instrumentId,
        OrderSide side,
        BigDecimal quantity,
        OrderType orderType,
        OrderStatus status,
        String clientOrderId,
        LocalDateTime createdAt
) {
}
