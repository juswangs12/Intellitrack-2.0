package com.intellitrack.dto.coordinator;

public record SummaryCardsDto(
    Long totalStudents,
    Long totalGroups,
    Long totalAdvisers,
    Long studentsSubmitted,
    Long studentsLate,
    Long studentsMissing,
    Double overallCompletionRate,
    Double overallSubmissionRate,
    Long totalDeliverables,
    Long pendingCount,
    Double institutionRiskScore
) {}
