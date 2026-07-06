package com.intellitrack.controller;

import com.intellitrack.dto.ApiResponse;
import com.intellitrack.dto.InsightHubDto;
import com.intellitrack.dto.CoordinatorAnalyticsSummaryDto;
import com.intellitrack.dto.StudentAnalyticsDto;
import com.intellitrack.dto.StudentDeliverableStatusDto;
import com.intellitrack.dto.SubjectAnalyticsDto;
import com.intellitrack.dto.SubmissionTrackingDashboardDto;
import com.intellitrack.dto.TrackingSnapshotDto;
import com.intellitrack.dto.coordinator.CoordinatorDashboardDto;
import com.intellitrack.service.AnalyticsFormatterService;
import com.intellitrack.service.MetricsCalculationService;
import com.intellitrack.service.SubmissionMetricsEngine;
import com.intellitrack.service.TrendVisualizationService;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.ByteArrayOutputStream;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
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

    @GetMapping("/students/export")
    public ResponseEntity<byte[]> exportStudentDeliverableStatuses() {
        try {
            List<StudentAnalyticsDto> students = metricsCalculationService.buildStudentAnalytics();

            try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
                Sheet sheet = workbook.createSheet("Student Deliverables");
                Row header = sheet.createRow(0);
                header.createCell(0).setCellValue("Full Name");
                header.createCell(1).setCellValue("Student ID");
                header.createCell(2).setCellValue("Group ID");
                header.createCell(3).setCellValue("Deliverable Name");

                int rowIndex = 1;
                for (StudentAnalyticsDto student : students) {
                    String fullName = safeText(student.studentName(), "Unknown Student");
                    String studentNumber = safeText(student.studentNumber(), String.valueOf(student.studentId()));
                    String groupIdentifier = student.groupCode() != null && !student.groupCode().isBlank()
                            ? student.groupCode()
                            : safeText(student.groupId() != null ? String.valueOf(student.groupId()) : null,
                                    "UNASSIGNED");

                    for (StudentDeliverableStatusDto deliverable : student.deliverableStatuses()) {
                        Row row = sheet.createRow(rowIndex++);
                        row.createCell(0).setCellValue(fullName);
                        row.createCell(1).setCellValue(studentNumber);
                        row.createCell(2).setCellValue(groupIdentifier);

                        String deliverableName = safeText(deliverable.deliverableName(), "Unnamed Deliverable");
                        String statusLabel = toExportStatusLabel(deliverable, LocalDateTime.now());
                        row.createCell(3).setCellValue(deliverableName + " - " + statusLabel);
                    }
                }

                for (int i = 0; i < 4; i++) {
                    sheet.autoSizeColumn(i);
                }

                workbook.write(out);
                String filename = "student-deliverable-statuses-" + LocalDate.now() + ".xlsx";

                return ResponseEntity.ok()
                        .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                        .contentType(MediaType
                                .parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                        .body(out.toByteArray());
            }
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
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

    private String safeText(String value, String fallback) {
        if (value == null || value.isBlank()) {
            return fallback;
        }
        return value;
    }

    private String toExportStatusLabel(StudentDeliverableStatusDto deliverable, LocalDateTime now) {
        String status = deliverable.status() == null ? "NOT_SUBMITTED" : deliverable.status();

        return switch (status) {
            case "SUBMITTED", "SUBMITTED_ON_TIME" -> "PASSED";
            case "SUBMITTED_LATE" -> {
                Integer days = deliverable.lateDays();
                if ((days == null || days <= 0) && deliverable.dueAt() != null && deliverable.submittedAt() != null) {
                    days = positiveDayDelta(deliverable.dueAt(), deliverable.submittedAt());
                }
                yield days != null && days > 0 ? "LATE (" + days + " DAYS)" : "LATE";
            }
            case "NOT_SUBMITTED", "PENDING" -> {
                Integer overdueDays = deliverable.dueAt() != null ? positiveDayDelta(deliverable.dueAt(), now) : null;
                if (overdueDays != null && overdueDays > 0) {
                    yield "DID NOT SUBMIT (" + overdueDays + " DAYS)";
                }
                yield "DEADLINE UPCOMING";
            }
            default -> status.replace('_', ' ');
        };
    }

    private Integer positiveDayDelta(LocalDateTime from, LocalDateTime to) {
        long days = Duration.between(from, to).toDays();
        if (days > 0) {
            return (int) days;
        }
        return null;
    }
}