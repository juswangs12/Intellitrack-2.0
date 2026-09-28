package com.intellitrack.dto.coordinator;

import java.time.LocalDateTime;

public record DeliverableMetaDto(
    Long id,
    String name,
    String stage,
    LocalDateTime dueAt,
    Long subjectId
) {}
