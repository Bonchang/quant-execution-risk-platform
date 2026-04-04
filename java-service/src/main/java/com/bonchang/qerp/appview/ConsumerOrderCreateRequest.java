package com.bonchang.qerp.appview;

import com.bonchang.qerp.order.OrderSide;
import com.bonchang.qerp.order.OrderType;
import com.bonchang.qerp.order.TimeInForce;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public record ConsumerOrderCreateRequest(
        @NotBlank String symbol,
        @NotNull OrderSide side,
        @NotNull @DecimalMin(value = "0.000001", inclusive = true) BigDecimal quantity,
        @NotNull OrderType orderType,
        @DecimalMin(value = "0.000001", inclusive = true) BigDecimal limitPrice,
        @NotNull TimeInForce timeInForce
) {
}
