package com.bonchang.qerp.order.dto;

import com.bonchang.qerp.order.OrderSide;
import com.bonchang.qerp.order.OrderType;
import com.bonchang.qerp.order.TimeInForce;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

public record CreateOrderRequest(
        @NotNull Long accountId,
        @NotNull Long strategyRunId,
        @NotNull Long instrumentId,
        @NotNull OrderSide side,
        @NotNull @DecimalMin(value = "0.000001", inclusive = true) BigDecimal quantity,
        @NotNull OrderType orderType,
        @DecimalMin(value = "0.000001", inclusive = true) BigDecimal limitPrice,
        @NotNull TimeInForce timeInForce,
        @NotBlank @Size(max = 64) String clientOrderId
) {
}
