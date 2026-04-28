package com.intellitrack.dto;

import java.util.List;

public record TrackingSnapshotDto(
        long totalDeliverables,
        long submitted,
        long pending,
        long late,
        List<ActivityFeedItemDto> activityFeed,
        List<GroupProgressDto> groupProgress) {
}