package com.intellitrack.dto;

import java.time.LocalDateTime;

public record DeliverableStatusDto(
                Long deliverableId,
                String deliverableName,
                String stage,
                String status,
                LocalDateTime dueAt,
                LocalDateTime submittedAt,
                long hoursRemaining,
                int revisionCount) {
}