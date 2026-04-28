package com.intellitrack.dto;

import java.util.List;

public record InsightHubDto(
        List<MetricCardDto> metricCards,
        List<ChartPointDto> trendSeries,
        List<ChartPointDto> statusBreakdown) {
}