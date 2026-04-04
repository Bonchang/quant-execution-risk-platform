package com.bonchang.qerp.outbox;

import com.bonchang.qerp.portfolio.PortfolioSnapshotService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDateTime;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
@Slf4j
public class OutboxEventProcessor {

    private final OutboxEventRepository outboxEventRepository;
    private final PortfolioSnapshotService portfolioSnapshotService;
    private final ObjectMapper objectMapper;

    @Scheduled(fixedDelayString = "${outbox.poll-ms:5000}")
    @Transactional
    public void processPendingEvents() {
        for (OutboxEvent event : outboxEventRepository.findByProcessingStatusOrderByIdAsc(
                OutboxProcessingStatus.PENDING,
                PageRequest.of(0, 50)
        )) {
            try {
                if (event.getEventType().contains("FILLED")) {
                    Long strategyRunId = extractStrategyRunId(event.getPayloadJson());
                    if (strategyRunId != null) {
                        portfolioSnapshotService.createSnapshotForStrategyRun(strategyRunId);
                    }
                }
                event.setProcessingStatus(OutboxProcessingStatus.PROCESSED);
                event.setProcessedAt(LocalDateTime.now());
            } catch (Exception ex) {
                log.warn("failed to process outbox event id={}", event.getId(), ex);
                event.setProcessingStatus(OutboxProcessingStatus.FAILED);
                event.setProcessedAt(LocalDateTime.now());
            }
        }
    }

    private Long extractStrategyRunId(String payloadJson) {
        try {
            Map<String, Object> payload = objectMapper.readValue(payloadJson, new TypeReference<>() {
            });
            Object value = payload.get("strategyRunId");
            if (value == null) {
                return null;
            }
            return Long.valueOf(String.valueOf(value));
        } catch (Exception ex) {
            log.warn("failed to parse outbox payload", ex);
            return null;
        }
    }
}
