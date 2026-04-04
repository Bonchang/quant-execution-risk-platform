package com.bonchang.qerp.appview;

import com.bonchang.qerp.appauth.AppSessionService;
import com.bonchang.qerp.appuser.AppUserService;
import com.bonchang.qerp.instrument.Instrument;
import com.bonchang.qerp.instrument.InstrumentRepository;
import com.bonchang.qerp.order.OrderPricingService;
import com.bonchang.qerp.order.OrderService;
import com.bonchang.qerp.order.OrderType;
import com.bonchang.qerp.order.dto.CreateOrderRequest;
import com.bonchang.qerp.order.dto.CreateOrderResponse;
import com.bonchang.qerp.order.dto.OrderDetailResponse;
import com.bonchang.qerp.strategyrun.StrategyRun;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class AppTradingService {

    private final AppSessionService appSessionService;
    private final AppUserService appUserService;
    private final InstrumentRepository instrumentRepository;
    private final OrderPricingService orderPricingService;
    private final OrderService orderService;

    @Transactional
    public CreateOrderResponse createOrder(ConsumerOrderCreateRequest request) {
        var principal = appSessionService.requirePrincipalWithAccount();
        Instrument instrument = instrumentRepository.findBySymbolIgnoreCase(request.symbol())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "symbol not found"));
        StrategyRun strategyRun = appUserService.ensureDefaultConsumerStrategyRun(
                appUserService.getActiveUser(principal.userId()).getAccount()
        );

        if (request.orderType() == OrderType.MARKET) {
            OrderPricingService.PriceSnapshot snapshot = orderPricingService.resolvePriceSnapshot(instrument.getId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "live quote not available"));
            if (!snapshot.quoteAvailable() || snapshot.stale()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "live quote required for market orders");
            }
        }

        return orderService.createOrder(
                new CreateOrderRequest(
                        principal.accountId(),
                        strategyRun.getId(),
                        instrument.getId(),
                        request.side(),
                        request.quantity(),
                        request.orderType(),
                        request.orderType() == OrderType.LIMIT ? request.limitPrice() : null,
                        request.timeInForce(),
                        "guest-" + principal.userId() + "-" + instrument.getSymbol() + "-" + System.currentTimeMillis()
                )
        );
    }

    @Transactional(readOnly = true)
    public OrderDetailResponse orderDetail(Long id) {
        var principal = appSessionService.requirePrincipalWithAccount();
        return orderService.getOrderDetailForAccount(id, principal.accountId());
    }

    @Transactional
    public CreateOrderResponse cancelOrder(Long id) {
        var principal = appSessionService.requirePrincipalWithAccount();
        return orderService.cancelOrderForAccount(id, principal.accountId());
    }
}
