package com.intellitrack.dto;

import java.time.LocalDateTime;

public record AuditLogDto(
        Long id,
        String action,
        String performedBy,
        String targetModule,
        String details,
        LocalDateTime timestamp,
        String ipAddress) {
}

