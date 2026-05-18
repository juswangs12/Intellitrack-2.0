package com.intellitrack.dto.coordinator;

public record GroupRiskDto(
    Long groupId,
    String groupTitle,
    String riskLevel,
    String reason,
    int missedDeadlines
) {}
