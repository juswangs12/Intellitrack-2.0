package com.intellitrack.dto;

import java.util.List;

public record AIInsightResponseDto(
        SubmissionAnalysis submissionAnalysis,
        List<String> issuesFound,
        RiskAssessment riskAssessment) {

    public record SubmissionAnalysis(
            String summary,
            String deadlineStatus,
            String deadlineExplanation,
            int completeness,
            String writingQuality,
            String organization,
            List<String> missingContent,
            List<String> recommendations) {
    }

    public record RiskAssessment(
            String riskLevel,
            String reason) {
    }
}
