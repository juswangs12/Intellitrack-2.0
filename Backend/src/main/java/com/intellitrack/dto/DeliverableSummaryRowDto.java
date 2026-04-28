package com.intellitrack.dto;

import java.time.LocalDateTime;

public record DeliverableSummaryRowDto(
        Long deliverableId,
        String deliverableName,
        String stage,
        String status,
        LocalDateTime submittedAt,
        int revisionCount) {
}