package com.intellitrack.dto;

import java.time.LocalDateTime;

public record DeadlineCardDto(
        Long deadlineId,
        Long deliverableId,
        String deliverableName,
        String stage,
        LocalDateTime dueAt,
        String status,
        long hoursRemaining,
        int revisionCount,
        int riskScore,
        String riskLevel,
        String riskExplanation,
        String countdownLabel) {
}