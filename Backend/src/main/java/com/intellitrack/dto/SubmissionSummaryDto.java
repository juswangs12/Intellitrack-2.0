package com.intellitrack.dto;

import java.util.List;

public record SubmissionSummaryDto(
        Long groupId,
        String groupCode,
        String groupTitle,
        String headlineSummary,
        String detailSummary,
        List<DeliverableSummaryRowDto> deliverables,
        List<ActivityFeedItemDto> timeline) {
}