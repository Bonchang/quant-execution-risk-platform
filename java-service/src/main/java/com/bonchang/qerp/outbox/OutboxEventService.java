package com.bonchang.qerp.outbox;

import com.bonchang.qerp.order.Order;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class OutboxEventService {

    private final OutboxEventRepository outboxEventRepository;
    private final ObjectMapper objectMapper;
    private final OutboxEventProcessor outboxEventProcessor;

    public void publishOrderEvent(Order order, String eventType) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("orderId", order.getId());
        payload.put("accountId", order.getAccount().getId());
        payload.put("strategyRunId", order.getStrategyRun().getId());
        payload.put("instrumentId", order.getInstrument().getId());
        payload.put("status", order.getStatus().name());
        payload.put("filledQuantity", order.getFilledQuantity());
        payload.put("remainingQuantity", order.getRemainingQuantity());
        payload.put("updatedAt", order.getUpdatedAt());

        OutboxEvent event = new OutboxEvent();
        event.setAggregateType("ORDER");
        event.setAggregateId(order.getId());
        event.setEventType(eventType);
        event.setPayloadJson(serialize(payload));
        event.setCreatedAt(LocalDateTime.now());
        event.setProcessingStatus(OutboxProcessingStatus.PENDING);
        outboxEventRepository.save(event);
        outboxEventProcessor.processPendingEvents();
    }

    private String serialize(Map<String, Object> payload) {
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("failed to serialize outbox payload", ex);
        }
    }
}
