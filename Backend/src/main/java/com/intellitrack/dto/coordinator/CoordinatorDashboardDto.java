package com.intellitrack.dto.coordinator;

import java.util.List;
import java.util.Map;

public record CoordinatorDashboardDto(
    Map<String, Long> submissionStats,
    List<AdviserWorkloadDto> adviserWorkload,
    List<GroupRiskDto> atRiskGroups,
    String aiStrategicInsight
) {}
