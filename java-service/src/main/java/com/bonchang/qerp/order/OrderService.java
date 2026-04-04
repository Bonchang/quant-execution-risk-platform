package com.bonchang.qerp.order;

import com.bonchang.qerp.account.Account;
import com.bonchang.qerp.account.AccountService;
import com.bonchang.qerp.execution.OrderExecutionService;
import com.bonchang.qerp.instrument.Instrument;
import com.bonchang.qerp.instrument.InstrumentRepository;
import com.bonchang.qerp.order.dto.CreateOrderRequest;
import com.bonchang.qerp.order.dto.CreateOrderResponse;
import com.bonchang.qerp.order.dto.ExpireOrdersResponse;
import com.bonchang.qerp.order.dto.OrderDetailResponse;
import com.bonchang.qerp.outbox.OutboxEventService;
import com.bonchang.qerp.position.Position;
import com.bonchang.qerp.position.PositionRepository;
import com.bonchang.qerp.risk.RiskEvaluationService;
import com.bonchang.qerp.strategyrun.StrategyRun;
import com.bonchang.qerp.strategyrun.StrategyRunRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
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
    private final JdbcTemplate jdbcTemplate;

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

    public OrderDetailResponse getOrderDetail(Long id) {
        List<OrderDetailResponse> rows = jdbcTemplate.query(
                """
                SELECT o.id,
                       o.account_id,
                       a.account_code,
                       a.owner_name,
                       a.base_currency,
                       cb.available_cash,
                       cb.reserved_cash,
                       o.strategy_run_id,
                       s.strategy_name,
                       o.instrument_id,
                       i.symbol AS instrument_symbol,
                       o.side,
                       o.quantity,
                       o.limit_price,
                       o.reserved_cash_amount,
                       o.filled_quantity,
                       o.remaining_quantity,
                       o.order_type,
                       o.time_in_force,
                       o.status,
                       o.client_order_id,
                       o.created_at,
                       o.expires_at,
                       o.last_executed_at,
                       o.updated_at
                FROM orders o
                JOIN account a ON a.id = o.account_id
                JOIN cash_balance cb ON cb.account_id = a.id
                JOIN strategy_run s ON s.id = o.strategy_run_id
                JOIN instrument i ON i.id = o.instrument_id
                WHERE o.id = ?
                """,
                (rs, rowNum) -> new OrderDetailResponse(
                        rs.getLong("id"),
                        rs.getLong("account_id"),
                        rs.getString("account_code"),
                        rs.getString("owner_name"),
                        rs.getString("base_currency"),
                        rs.getBigDecimal("available_cash"),
                        rs.getBigDecimal("reserved_cash"),
                        rs.getLong("strategy_run_id"),
                        rs.getString("strategy_name"),
                        rs.getLong("instrument_id"),
                        rs.getString("instrument_symbol"),
                        OrderSide.valueOf(rs.getString("side")),
                        rs.getBigDecimal("quantity"),
                        rs.getBigDecimal("limit_price"),
                        rs.getBigDecimal("reserved_cash_amount"),
                        rs.getBigDecimal("filled_quantity"),
                        rs.getBigDecimal("remaining_quantity"),
                        OrderType.valueOf(rs.getString("order_type")),
                        TimeInForce.valueOf(rs.getString("time_in_force")),
                        OrderStatus.valueOf(rs.getString("status")),
                        rs.getString("client_order_id"),
                        rs.getTimestamp("created_at").toLocalDateTime(),
                        toDateTime(rs.getTimestamp("expires_at")),
                        toDateTime(rs.getTimestamp("last_executed_at")),
                        rs.getTimestamp("updated_at").toLocalDateTime(),
                        loadFills(id),
                        loadRiskChecks(id),
                        loadOutboxEvents(id),
                        loadCashLedger(id)
                ),
                id
        );
        if (rows.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "order not found");
        }
        return rows.get(0);
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

    private List<OrderDetailResponse.FillItem> loadFills(Long orderId) {
        return jdbcTemplate.query(
                """
                SELECT id, fill_quantity, fill_price, filled_at
                FROM fill
                WHERE order_id = ?
                ORDER BY filled_at ASC, id ASC
                """,
                (rs, rowNum) -> new OrderDetailResponse.FillItem(
                        rs.getLong("id"),
                        rs.getBigDecimal("fill_quantity"),
                        rs.getBigDecimal("fill_price"),
                        rs.getTimestamp("filled_at").toLocalDateTime()
                ),
                orderId
        );
    }

    private List<OrderDetailResponse.RiskCheckItem> loadRiskChecks(Long orderId) {
        return jdbcTemplate.query(
                """
                SELECT id, rule_name, passed, message, checked_at
                FROM risk_check_result
                WHERE order_id = ?
                ORDER BY checked_at ASC, id ASC
                """,
                (rs, rowNum) -> new OrderDetailResponse.RiskCheckItem(
                        rs.getLong("id"),
                        rs.getString("rule_name"),
                        rs.getBoolean("passed"),
                        rs.getString("message"),
                        rs.getTimestamp("checked_at").toLocalDateTime()
                ),
                orderId
        );
    }

    private List<OrderDetailResponse.OutboxEventItem> loadOutboxEvents(Long orderId) {
        return jdbcTemplate.query(
                """
                SELECT id, aggregate_type, aggregate_id, event_type, processing_status, created_at, processed_at
                FROM outbox_event
                WHERE aggregate_type = 'ORDER'
                  AND aggregate_id = ?
                ORDER BY created_at DESC, id DESC
                """,
                (rs, rowNum) -> new OrderDetailResponse.OutboxEventItem(
                        rs.getLong("id"),
                        rs.getString("aggregate_type"),
                        rs.getLong("aggregate_id"),
                        rs.getString("event_type"),
                        rs.getString("processing_status"),
                        rs.getTimestamp("created_at").toLocalDateTime(),
                        toDateTime(rs.getTimestamp("processed_at"))
                ),
                orderId
        );
    }

    private List<OrderDetailResponse.CashLedgerItem> loadCashLedger(Long orderId) {
        return jdbcTemplate.query(
                """
                SELECT id, entry_type, amount, available_cash_after, reserved_cash_after, note, created_at
                FROM cash_ledger_entry
                WHERE order_id = ?
                ORDER BY created_at DESC, id DESC
                """,
                (rs, rowNum) -> new OrderDetailResponse.CashLedgerItem(
                        rs.getLong("id"),
                        rs.getString("entry_type"),
                        rs.getBigDecimal("amount"),
                        rs.getBigDecimal("available_cash_after"),
                        rs.getBigDecimal("reserved_cash_after"),
                        rs.getString("note"),
                        rs.getTimestamp("created_at").toLocalDateTime()
                ),
                orderId
        );
    }

    private LocalDateTime toDateTime(Timestamp timestamp) {
        return timestamp != null ? timestamp.toLocalDateTime() : null;
    }
}
