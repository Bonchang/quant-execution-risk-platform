package com.bonchang.qerp.outbox;

import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OutboxEventRepository extends JpaRepository<OutboxEvent, Long> {

    List<OutboxEvent> findByProcessingStatusOrderByIdAsc(OutboxProcessingStatus processingStatus, Pageable pageable);

    List<OutboxEvent> findAllByOrderByCreatedAtDescIdDesc(Pageable pageable);
}
