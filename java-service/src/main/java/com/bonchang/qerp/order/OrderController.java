package com.bonchang.qerp.order;

import com.bonchang.qerp.order.dto.CreateOrderRequest;
import com.bonchang.qerp.order.dto.CreateOrderResponse;
import com.bonchang.qerp.order.dto.ExpireOrdersResponse;
import com.bonchang.qerp.order.dto.OrderDetailResponse;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CreateOrderResponse createOrder(@Valid @RequestBody CreateOrderRequest request) {
        return orderService.createOrder(request);
    }

    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    public List<CreateOrderResponse> listOrders(@RequestParam(defaultValue = "20") int limit) {
        return orderService.listOrders(limit);
    }

    @GetMapping("/{id}")
    public OrderDetailResponse getOrder(@PathVariable Long id) {
        return orderService.getOrderDetail(id);
    }

    @PostMapping("/{id}/cancel")
    public CreateOrderResponse cancelOrder(@PathVariable Long id) {
        return orderService.cancelOrder(id);
    }

    @PostMapping("/expire-working")
    public ExpireOrdersResponse expireWorkingOrders() {
        return orderService.expireWorkingOrders();
    }
}
