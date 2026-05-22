package com.intellitrack.dto;

import java.util.List;

public record AIInsightResponseDto(
        SubmissionAnalysis submissionAnalysis,
        DeadlineStatus deadlineStatus,
        ProgressEvaluation progressEvaluation,
        List<String> issuesFound,
        List<String> recommendations,
        List<String> smartReminders,
        RiskAssessment riskAssessment) {

    public record SubmissionAnalysis(
            String summary,
            String technicalQuality,
            String documentationQuality,
            int completenessScore,
            List<String> majorFindings) {
    }

    public record DeadlineStatus(
            String status,
            String explanation) {
    }

    public record ProgressEvaluation(
            String progressStatus,
            String revisionAssessment) {
    }

    public record RiskAssessment(
            String riskLevel,
            String reason) {
    }
}
