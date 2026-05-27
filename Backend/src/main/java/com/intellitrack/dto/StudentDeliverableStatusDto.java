package com.intellitrack.dto;

import java.time.LocalDateTime;

public record StudentDeliverableStatusDto(
        Long deliverableId,
        String deliverableName,
        String stage,
        String status,
        LocalDateTime submittedAt,
        Integer lateDays
) {
}
