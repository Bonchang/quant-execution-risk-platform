package com.bonchang.qerp.order;

import com.bonchang.qerp.account.Account;
import com.bonchang.qerp.account.AccountService;
import com.bonchang.qerp.execution.OrderExecutionService;
import com.bonchang.qerp.instrument.Instrument;
import com.bonchang.qerp.instrument.InstrumentRepository;
import com.bonchang.qerp.order.dto.CreateOrderRequest;
import com.bonchang.qerp.order.dto.CreateOrderResponse;
import com.bonchang.qerp.order.dto.ExpireOrdersResponse;
import com.bonchang.qerp.outbox.OutboxEventService;
import com.bonchang.qerp.position.Position;
import com.bonchang.qerp.position.PositionRepository;
import com.bonchang.qerp.risk.RiskEvaluationService;
import com.bonchang.qerp.strategyrun.StrategyRun;
import com.bonchang.qerp.strategyrun.StrategyRunRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final StrategyRunRepository strategyRunRepository;
    private final InstrumentRepository instrumentRepository;
    private final PositionRepository positionRepository;
    private final AccountService accountService;
    private final RiskEvaluationService riskEvaluationService;
    private final OrderExecutionService orderExecutionService;
    private final OrderPricingService orderPricingService;
    private final OutboxEventService outboxEventService;

    @PersistenceContext
    private EntityManager entityManager;

    @Transactional
    public CreateOrderResponse createOrder(CreateOrderRequest request) {
        validateOrderRequest(request);
        StrategyRun strategyRun = strategyRunRepository.findById(request.strategyRunId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "strategyRunId not found"));
        if (!instrumentRepository.existsById(request.instrumentId())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "instrumentId not found");
        }
        Account account = accountService.getReferenceAccount(request.accountId());
        if (!strategyRun.getAccount().getId().equals(request.accountId())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "strategyRunId does not belong to accountId");
        }

        lockResources(strategyRun, request.instrumentId(), account);

        Order order = new Order();
        order.setAccount(entityManager.getReference(Account.class, request.accountId()));
        order.setStrategyRun(entityManager.getReference(StrategyRun.class, request.strategyRunId()));
        order.setInstrument(entityManager.getReference(Instrument.class, request.instrumentId()));
        order.setSide(request.side());
        order.setQuantity(request.quantity());
        order.setOrderType(request.orderType());
        order.setLimitPrice(request.orderType() == OrderType.LIMIT ? request.limitPrice() : null);
        order.setReservedCashAmount(BigDecimal.ZERO.setScale(6));
        order.setTimeInForce(request.timeInForce());
        order.setStatus(OrderStatus.CREATED);
        order.setClientOrderId(request.clientOrderId());
        order.setCreatedAt(LocalDateTime.now());
        order.setFilledQuantity(BigDecimal.ZERO.setScale(6));
        order.setRemainingQuantity(request.quantity());
        order.setExpiresAt(resolveExpiresAt(order.getCreatedAt(), request.timeInForce()));
        order.setUpdatedAt(order.getCreatedAt());

        Order saved;
        try {
            saved = orderRepository.save(order);
        } catch (DataIntegrityViolationException ex) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "duplicate clientOrderId for strategyRunId", ex);
        }

        Order evaluated = riskEvaluationService.evaluateAndUpdateOrderStatus(saved);
        if (evaluated.getStatus() == OrderStatus.REJECTED) {
            outboxEventService.publishOrderEvent(evaluated, "ORDER_REJECTED");
            return toResponse(evaluated);
        }

        if (evaluated.getSide() == OrderSide.BUY) {
            BigDecimal estimatedReservation = orderPricingService.calculateEstimatedBuyNotional(evaluated);
            accountService.reserveBuyCash(
                    account,
                    evaluated,
                    estimatedReservation,
                    "Reserve buy cash for order " + evaluated.getId()
            );
        }

        Order executed = orderExecutionService.executeApprovedOrder(evaluated);
        return toResponse(executed);
    }

    public List<CreateOrderResponse> listOrders(int limit) {
        return orderRepository.findAllByOrderByIdDesc(PageRequest.of(0, Math.min(Math.max(limit, 1), 100)))
                .stream()
                .map(this::toResponse)
                .toList();
    }

    public CreateOrderResponse getOrder(Long id) {
        return orderRepository.findById(id)
                .map(this::toResponse)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "order not found"));
    }

    @Transactional
    public CreateOrderResponse cancelOrder(Long id) {
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "order not found"));
        if (order.getStatus() != OrderStatus.WORKING) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "only WORKING orders can be canceled");
        }
        if (order.getReservedCashAmount().compareTo(BigDecimal.ZERO) > 0) {
            accountService.releaseReservedCash(
                    order.getAccount(),
                    order,
                    order.getReservedCashAmount(),
                    "Release canceled order reservation for order " + order.getId()
            );
        }
        order.setStatus(OrderStatus.CANCELED);
        order.setUpdatedAt(LocalDateTime.now());
        Order saved = orderRepository.save(order);
        outboxEventService.publishOrderEvent(saved, "ORDER_CANCELED");
        return toResponse(saved);
    }

    @Transactional
    public ExpireOrdersResponse expireWorkingOrders() {
        List<Order> orders = orderRepository.findByStatusAndTimeInForceAndExpiresAtBefore(
                OrderStatus.WORKING,
                TimeInForce.DAY,
                LocalDateTime.now()
        );
        for (Order order : orders) {
            if (order.getReservedCashAmount().compareTo(BigDecimal.ZERO) > 0) {
                accountService.releaseReservedCash(
                        order.getAccount(),
                        order,
                        order.getReservedCashAmount(),
                        "Release expired order reservation for order " + order.getId()
                );
            }
            order.setStatus(OrderStatus.EXPIRED);
            order.setUpdatedAt(LocalDateTime.now());
            outboxEventService.publishOrderEvent(order, "ORDER_EXPIRED");
        }
        return new ExpireOrdersResponse(orders.size(), "Expired " + orders.size() + " DAY working orders");
    }

    private void validateOrderRequest(CreateOrderRequest request) {
        if (request.orderType() == OrderType.LIMIT && request.limitPrice() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "limitPrice is required for LIMIT order");
        }
        if (request.orderType() == OrderType.MARKET && request.limitPrice() != null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "limitPrice must be null for MARKET order");
        }
    }

    private void lockResources(StrategyRun strategyRun, Long instrumentId, Account account) {
        accountService.lockCashBalance(account.getId());
        Position locked = positionRepository.lockByStrategyRunIdAndInstrumentId(strategyRun.getId(), instrumentId)
                .orElse(null);
        if (locked == null) {
            Position position = new Position();
            position.setAccount(account);
            position.setStrategyRun(strategyRun);
            position.setInstrument(entityManager.getReference(Instrument.class, instrumentId));
            position.setNetQuantity(BigDecimal.ZERO.setScale(6));
            position.setAveragePrice(BigDecimal.ZERO.setScale(6));
            position.setUpdatedAt(LocalDateTime.now());
            try {
                positionRepository.saveAndFlush(position);
            } catch (DataIntegrityViolationException ignored) {
                // another transaction inserted the lock row first
            }
            positionRepository.lockByStrategyRunIdAndInstrumentId(strategyRun.getId(), instrumentId);
        }
    }

    private LocalDateTime resolveExpiresAt(LocalDateTime createdAt, TimeInForce timeInForce) {
        if (timeInForce == TimeInForce.GTC) {
            return null;
        }
        return createdAt.toLocalDate().atTime(LocalTime.of(23, 59, 59));
    }

    private CreateOrderResponse toResponse(Order order) {
        return new CreateOrderResponse(
                order.getId(),
                order.getAccount().getId(),
                order.getStrategyRun().getId(),
                order.getInstrument().getId(),
                order.getSide(),
                order.getQuantity(),
                order.getLimitPrice(),
                order.getReservedCashAmount(),
                order.getFilledQuantity(),
                order.getRemainingQuantity(),
                order.getOrderType(),
                order.getTimeInForce(),
                order.getStatus(),
                order.getClientOrderId(),
                order.getCreatedAt(),
                order.getExpiresAt(),
                order.getLastExecutedAt(),
                order.getUpdatedAt()
        );
    }
}
