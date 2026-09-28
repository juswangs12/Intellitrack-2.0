package com.intellitrack.dto.coordinator;

public record GroupMetaDto(
    Long id,
    String code,
    String title,
    Long adviserId,
    Long subjectId
) {}
