package com.intellitrack.dto;

import java.util.List;

public record RawSubmissionSummaryData(
        Long groupId,
        String groupCode,
        String groupTitle,
        Long subjectId,
        String subjectCode,
        String subjectName,
        List<DeliverableSummaryRowDto> deliverables,
        List<ActivityFeedItemDto> timeline) {
}