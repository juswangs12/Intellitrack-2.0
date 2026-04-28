package com.intellitrack.dto;

import java.util.List;

public record SubmissionTrackingDashboardDto(
        List<MetricCardDto> metricCards,
        List<ChartPointDto> statusDistribution,
        List<ActivityFeedItemDto> activityFeed,
        List<GroupProgressDto> groupProgress) {
}