package com.intellitrack.dto;

import java.util.List;

public record CoordinatorAnalyticsSummaryDto(
        List<SubjectAnalyticsDto> subjectAnalytics,
        List<StudentAnalyticsDto> studentAnalytics,
        String aiSummary
) {
}
