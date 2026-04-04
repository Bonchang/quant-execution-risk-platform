package com.bonchang.qerp.appview;

import com.bonchang.qerp.order.dto.CreateOrderResponse;
import com.bonchang.qerp.order.dto.OrderDetailResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/app/orders")
@RequiredArgsConstructor
public class AppTradingController {

    private final AppTradingService appTradingService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CreateOrderResponse createOrder(@Valid @RequestBody ConsumerOrderCreateRequest request) {
        return appTradingService.createOrder(request);
    }

    @GetMapping("/{id}")
    public OrderDetailResponse getOrder(@PathVariable Long id) {
        return appTradingService.orderDetail(id);
    }

    @PostMapping("/{id}/cancel")
    public CreateOrderResponse cancelOrder(@PathVariable Long id) {
        return appTradingService.cancelOrder(id);
    }
}
