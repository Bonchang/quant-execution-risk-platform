package com.bonchang.qerp.appview;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record OrdersScreenResponse(
        Summary summary,
        List<OrderItem> orders
) {

    public record Summary(
            long totalOrders,
            long filledOrders,
            long workingOrders,
            long rejectedOrders
    ) {
    }

    public record OrderItem(
            Long id,
            String symbol,
            String side,
            String status,
            BigDecimal quantity,
            BigDecimal filledQuantity,
            BigDecimal limitPrice,
            String orderType,
            String accountCode,
            LocalDateTime updatedAt,
            boolean cancelable
    ) {
    }
}
