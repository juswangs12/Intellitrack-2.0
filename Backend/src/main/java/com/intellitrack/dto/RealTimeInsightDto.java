package com.intellitrack.dto;

import java.util.List;

public record RealTimeInsightDto(
        double onTimePercentage,
        double latePercentage,
        double pendingPercentage,
        List<ChartPointDto> trendSeries,
        List<ChartPointDto> statusBreakdown) {
}