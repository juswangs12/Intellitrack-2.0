package com.intellitrack.controller;

import com.intellitrack.dto.ApiResponse;
import com.intellitrack.dto.InsightHubDto;
import com.intellitrack.dto.CoordinatorAnalyticsSummaryDto;
import com.intellitrack.dto.StudentAnalyticsDto;
import com.intellitrack.dto.SubjectAnalyticsDto;
import com.intellitrack.dto.SubmissionTrackingDashboardDto;
import com.intellitrack.dto.TrackingSnapshotDto;
import com.intellitrack.dto.coordinator.CoordinatorDashboardDto;
import com.intellitrack.dto.coordinator.CoordinatorAnalyticsV2Dto;
import com.intellitrack.service.AnalyticsFormatterService;
import com.intellitrack.service.MetricsCalculationService;
import com.intellitrack.service.SubmissionMetricsEngine;
import com.intellitrack.service.TrendVisualizationService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/analytics")
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

    @GetMapping("/subjects")
    public ResponseEntity<List<SubjectAnalyticsDto>> getSubjectAnalytics() {
        return ResponseEntity.ok(metricsCalculationService.buildSubjectAnalytics());
    }

    @GetMapping("/students")
    public ResponseEntity<List<StudentAnalyticsDto>> getStudentAnalytics() {
        return ResponseEntity.ok(metricsCalculationService.buildStudentAnalytics());
    }

    @GetMapping("/coordinator-summary")
    public ResponseEntity<CoordinatorAnalyticsSummaryDto> getCoordinatorAnalyticsSummary() {
        return ResponseEntity.ok(metricsCalculationService.buildCoordinatorAnalyticsSummary());
    }

    @PostMapping("/generate-coordinator-ai-summary")
    public ResponseEntity<ApiResponse<String>> generateCoordinatorAiSummary() {
        try {
            String summary = metricsCalculationService.generateCoordinatorAiSummary();
            return ResponseEntity.ok(ApiResponse.success(summary));
        } catch (Exception e) {
            System.err.println("Error generating coordinator AI summary: " + e.getMessage());
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error("Failed to generate AI summary: " + e.getMessage()));
        }
    }
    
    @GetMapping("/coordinator-v2")
    public ResponseEntity<CoordinatorAnalyticsV2Dto> getCoordinatorAnalyticsV2() {
        return ResponseEntity.ok(metricsCalculationService.buildCoordinatorAnalyticsV2());
    }
}