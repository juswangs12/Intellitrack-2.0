package com.intellitrack.dto;

import java.time.LocalDateTime;

public record SubmissionVersionDto(
        Long id,
        Long submissionId,
        Integer versionNumber,
        String status,
        LocalDateTime submittedAt,
        String notes,
        String fileName,
        String aiSummary,
        String aiHighlights) {
}

