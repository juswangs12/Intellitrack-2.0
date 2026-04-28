package com.intellitrack.dto;

public record ReminderDto(
        Long groupId,
        Long deliverableId,
        String deliverableName,
        String message,
        String riskLevel,
        long hoursRemaining) {
}