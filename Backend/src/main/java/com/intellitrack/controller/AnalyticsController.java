package com.intellitrack.controller;

import com.intellitrack.dto.InsightHubDto;
import com.intellitrack.dto.SubmissionTrackingDashboardDto;
import com.intellitrack.dto.TrackingSnapshotDto;
import com.intellitrack.dto.coordinator.CoordinatorDashboardDto;
import com.intellitrack.service.AnalyticsFormatterService;
import com.intellitrack.service.MetricsCalculationService;
import com.intellitrack.service.SubmissionMetricsEngine;
import com.intellitrack.service.TrendVisualizationService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/analytics")
@CrossOrigin(origins = "http://localhost:3000")
public class AnalyticsController {

    private final MetricsCalculationService metricsCalculationService;
    private final AnalyticsFormatterService analyticsFormatterService;
    private final SubmissionMetricsEngine submissionMetricsEngine;
    private final TrendVisualizationService trendVisualizationService;

    public AnalyticsController(
            MetricsCalculationService metricsCalculationService,
            AnalyticsFormatterService analyticsFormatterService,
            SubmissionMetricsEngine submissionMetricsEngine,
            TrendVisualizationService trendVisualizationService) {
        this.metricsCalculationService = metricsCalculationService;
        this.analyticsFormatterService = analyticsFormatterService;
        this.submissionMetricsEngine = submissionMetricsEngine;
        this.trendVisualizationService = trendVisualizationService;
    }

    @GetMapping("/tracking")
    public ResponseEntity<SubmissionTrackingDashboardDto> getTracking(@RequestParam(required = false) Long adviserId) {
        TrackingSnapshotDto snapshot = metricsCalculationService.buildTrackingSnapshot(adviserId);
        return ResponseEntity.ok(analyticsFormatterService.formatTracking(snapshot));
    }

    @GetMapping("/insights")
    public ResponseEntity<InsightHubDto> getInsights(
            @RequestParam(required = false) String stage,
            @RequestParam(required = false) Long adviserId) {
        return ResponseEntity
                .ok(trendVisualizationService.toInsightHub(submissionMetricsEngine.computeInsights(stage, adviserId)));
    }

    @GetMapping("/coordinator")
    public ResponseEntity<CoordinatorDashboardDto> getCoordinatorDashboard() {
        return ResponseEntity.ok(metricsCalculationService.buildCoordinatorDashboard());
    }
}