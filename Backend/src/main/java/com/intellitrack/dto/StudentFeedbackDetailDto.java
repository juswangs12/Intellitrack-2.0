package com.intellitrack.dto;

import java.time.LocalDateTime;
import java.util.List;

public record StudentFeedbackDetailDto(
        Long submissionId,
        String status,
        Double totalScore,
        String generalComments,
        LocalDateTime evaluatedAt,
        String evaluatorName,
        List<Criterion> criteria) {

    public record Criterion(
            Long criterionId,
            String name,
            Integer score,
            Integer maxPoints,
            Integer weight,
            String comments) {
    }
}

