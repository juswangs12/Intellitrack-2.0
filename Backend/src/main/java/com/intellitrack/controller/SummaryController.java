package com.intellitrack.controller;

import com.intellitrack.dto.RawSubmissionSummaryData;
import com.intellitrack.dto.SubmissionSummaryDto;
import com.intellitrack.dto.SummaryInsightDto;
import com.intellitrack.service.AISummaryEngine;
import com.intellitrack.service.SubmissionDataService;
import com.intellitrack.service.SummaryFormatterService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/submission-summary")
@CrossOrigin(origins = "http://localhost:3000")
public class SummaryController {

    private final SubmissionDataService submissionDataService;
    private final AISummaryEngine aiSummaryEngine;
    private final SummaryFormatterService summaryFormatterService;

    public SummaryController(
            SubmissionDataService submissionDataService,
            AISummaryEngine aiSummaryEngine,
            SummaryFormatterService summaryFormatterService) {
        this.submissionDataService = submissionDataService;
        this.aiSummaryEngine = aiSummaryEngine;
        this.summaryFormatterService = summaryFormatterService;
    }

    @GetMapping
    public ResponseEntity<SubmissionSummaryDto> getSummary(@RequestParam Long groupId) {
        RawSubmissionSummaryData rawData = submissionDataService.fetchByGroupId(groupId);
        SummaryInsightDto insight = aiSummaryEngine.generate(rawData);
        return ResponseEntity.ok(summaryFormatterService.format(rawData, insight));
    }
}