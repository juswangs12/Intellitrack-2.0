package com.intellitrack.dto;

import java.time.LocalDateTime;

public record DeadlineAdminDto(
        Long deliverableId,
        String deliverableName,
        String stage,
        boolean active,
        Long deadlineId,
        LocalDateTime dueAt,
        String academicTerm) {
}

