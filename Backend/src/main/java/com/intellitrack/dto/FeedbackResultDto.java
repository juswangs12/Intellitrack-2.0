package com.intellitrack.dto;

import java.time.LocalDateTime;

public record FeedbackResultDto(
        Long submissionId,
        String status,
        Double totalScore,
        LocalDateTime evaluatedAt) {
}

