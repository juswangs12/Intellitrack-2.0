package com.intellitrack.dto.coordinator;

public record AdviserWorkloadDto(
    Long adviserId,
    String name,
    long assignedGroups,
    long pendingReviews,
    double completionRate
) {}
