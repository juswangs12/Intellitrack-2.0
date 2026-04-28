package com.intellitrack.dto;

import java.time.LocalDateTime;

public record CalendarDeadlineDto(
        Long deadlineId,
        Long deliverableId,
        String deliverableName,
        String stage,
        LocalDateTime dueAt) {
}