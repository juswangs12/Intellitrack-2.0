package com.intellitrack.dto;

import java.util.List;

public record RawSubmissionSummaryData(
        Long groupId,
        String groupCode,
        String groupTitle,
        List<DeliverableSummaryRowDto> deliverables,
        List<ActivityFeedItemDto> timeline) {
}