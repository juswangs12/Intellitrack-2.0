package com.intellitrack.dto;

import java.time.LocalDateTime;
import java.util.List;

public record StudentWorkspaceDto(
        Header header,
        SubmissionOverview submissionOverview,
        List<DeliverableRow> deliverables,
        List<DeadlineCardDto> upcomingDeadlines,
        List<ReminderDto> aiReminders,
        String aiInsight,
        List<FeedbackItem> recentFeedback,
        List<TimelineItem> timeline) {

    public record Header(
            Long studentId,
            String studentName,
            Long groupId,
            String groupCode,
            String groupTitle,
            Long adviserId,
            String adviserName) {
    }

    public record SubmissionOverview(
            long totalDeliverables,
            long submittedDeliverables,
            long pendingDeliverables,
            long lateDeliverables,
            long underReviewDeliverables,
            long needsRevisionDeliverables,
            long approvedDeliverables,
            long rejectedDeliverables) {
    }

    public record DeliverableRow(
            Long deliverableId,
            String deliverableName,
            String stage,
            String status,
            LocalDateTime dueAt,
            Long hoursRemaining,
            Integer revisionCount,
            LocalDateTime submittedAt,
            Long submissionId,
            Integer versionNumber,
            boolean hasAiSummary) {
    }

    public record FeedbackItem(
            Long submissionId,
            Long deliverableId,
            String deliverableName,
            String status,
            Double totalScore,
            String generalComments,
            LocalDateTime evaluatedAt,
            String evaluatorName) {
    }

    public record TimelineItem(
            String type,
            String title,
            String detail,
            LocalDateTime occurredAt,
            Long deliverableId,
            Long submissionId) {
    }
}

