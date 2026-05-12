package com.intellitrack.dto;

import java.time.LocalDateTime;
import java.util.List;

public record SubmissionDto(
        Long id,
        Long groupId,
        String groupCode,
        String groupTitle,
        List<StudentEnrollmentDto> students,
        Long deliverableId,
        String deliverableName,
        String stage,
        String status,
        LocalDateTime submittedAt,
        Integer versionNumber,
        Integer revisionCount,
        String notes,
        String aiSummary,
        String aiHighlights) {
}

