package com.bonchang.qerp.order;

import com.bonchang.qerp.instrument.Instrument;
import com.bonchang.qerp.instrument.InstrumentRepository;
import com.bonchang.qerp.order.dto.CreateOrderRequest;
import com.bonchang.qerp.order.dto.CreateOrderResponse;
import com.bonchang.qerp.risk.RiskEvaluationService;
import com.bonchang.qerp.strategyrun.StrategyRun;
import com.bonchang.qerp.strategyrun.StrategyRunRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
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
    private final RiskEvaluationService riskEvaluationService;

    @PersistenceContext
    private EntityManager entityManager;

    @Transactional
    public CreateOrderResponse createOrder(CreateOrderRequest request) {
        if (!strategyRunRepository.existsById(request.strategyRunId())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "strategyRunId not found");
        }
        if (!instrumentRepository.existsById(request.instrumentId())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "instrumentId not found");
        }

        Order order = new Order();
        order.setStrategyRun(entityManager.getReference(StrategyRun.class, request.strategyRunId()));
        order.setInstrument(entityManager.getReference(Instrument.class, request.instrumentId()));
        order.setSide(request.side());
        order.setQuantity(request.quantity());
        order.setOrderType(request.orderType());
        order.setStatus(OrderStatus.CREATED);
        order.setClientOrderId(request.clientOrderId());
        order.setCreatedAt(LocalDateTime.now());

<<<<<<< ours
        Order saved = orderRepository.save(order);
        Order evaluated = riskEvaluationService.evaluateAndUpdateOrderStatus(saved);
=======
        Order saved;
        try {
            saved = orderRepository.save(order);
        } catch (DataIntegrityViolationException ex) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "duplicate clientOrderId for strategyRunId", ex);
        }
>>>>>>> theirs

        return new CreateOrderResponse(
                evaluated.getId(),
                request.strategyRunId(),
                request.instrumentId(),
                evaluated.getSide(),
                evaluated.getQuantity(),
                evaluated.getOrderType(),
                evaluated.getStatus(),
                evaluated.getClientOrderId(),
                evaluated.getCreatedAt()
        );
    }
}
