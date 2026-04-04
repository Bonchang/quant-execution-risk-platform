package com.bonchang.qerp.dashboard;

import java.time.LocalDateTime;
import java.util.List;

public record DashboardTimelineResponse(
        List<TimelineEventItem> events
) {

    public record TimelineEventItem(
            String category,
            String eventType,
            String severity,
            String title,
            String description,
            String subjectKey,
            LocalDateTime occurredAt
    ) {
    }
}
