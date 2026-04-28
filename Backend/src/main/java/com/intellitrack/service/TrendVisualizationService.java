package com.intellitrack.service;

import com.intellitrack.dto.InsightHubDto;
import com.intellitrack.dto.MetricCardDto;
import com.intellitrack.dto.RealTimeInsightDto;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class TrendVisualizationService {

    public InsightHubDto toInsightHub(RealTimeInsightDto insight) {
        return new InsightHubDto(
                List.of(
                        new MetricCardDto("On-Time %", insight.onTimePercentage(), "success"),
                        new MetricCardDto("Late %", insight.latePercentage(), "danger"),
                        new MetricCardDto("Pending %", insight.pendingPercentage(), "warning")),
                insight.trendSeries(),
                insight.statusBreakdown());
    }
}