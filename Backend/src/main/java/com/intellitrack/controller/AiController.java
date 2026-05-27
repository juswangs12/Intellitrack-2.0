package com.intellitrack.controller;

import com.intellitrack.dto.ApiResponse;
import com.intellitrack.entity.Submission;
import com.intellitrack.entity.SubmissionVersion;
import com.intellitrack.repository.SubmissionRepository;
import com.intellitrack.repository.SubmissionVersionRepository;
import com.intellitrack.service.AISummaryEngine;
import com.intellitrack.service.AiService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@SuppressWarnings("null")
@RestController
@RequestMapping("/api/ai")
public class AiController {

    @Autowired
    private AiService aiService;

    @Autowired
    private AISummaryEngine aiSummaryEngine;

    @Autowired
    private SubmissionRepository submissionRepository;

    @Autowired
    private SubmissionVersionRepository submissionVersionRepository;

    @PostMapping("/analyze-submission/{submissionId}")
    public ResponseEntity<ApiResponse<String>> analyzeSubmission(@PathVariable Long submissionId) {
        Submission submission = submissionRepository.findById(submissionId).orElse(null);
        if (submission == null) {
            return ResponseEntity.badRequest().body(ApiResponse.error("Submission not found"));
        }

        aiSummaryEngine.processDocument(submission);
        return ResponseEntity.ok(ApiResponse.success("AI analysis triggered for submission " + submissionId));
    }

    @PostMapping("/analyze-submission/{submissionId}/version/{versionId}")
    public ResponseEntity<ApiResponse<String>> analyzeSubmissionWithVersion(
            @PathVariable Long submissionId,
            @PathVariable Long versionId) {
        Submission submission = submissionRepository.findById(submissionId).orElse(null);
        SubmissionVersion version = submissionVersionRepository.findById(versionId).orElse(null);

        if (submission == null) {
            return ResponseEntity.badRequest().body(ApiResponse.error("Submission not found"));
        }
        if (version == null) {
            return ResponseEntity.badRequest().body(ApiResponse.error("Submission version not found"));
        }

        aiSummaryEngine.processDocument(submission, versionId, false);
        return ResponseEntity.ok(ApiResponse
                .success("AI analysis triggered for submission " + submissionId + " and version " + versionId));
    }

    @PostMapping("/regenerate/{submissionId}")
    public ResponseEntity<ApiResponse<String>> regenerateAnalysis(@PathVariable Long submissionId) {
        Submission submission = submissionRepository.findById(submissionId).orElse(null);
        if (submission == null) {
            return ResponseEntity.badRequest().body(ApiResponse.error("Submission not found"));
        }

        aiSummaryEngine.processDocument(submission, true);
        return ResponseEntity.ok(ApiResponse.success("AI analysis regeneration triggered for submission " + submissionId));
    }

    @PostMapping("/generate-insight")
    public ResponseEntity<ApiResponse<String>> generateInsight(@RequestBody String prompt) {
        try {
            if (prompt == null || prompt.isBlank()) {
                return ResponseEntity.badRequest().body(ApiResponse.error("Prompt must not be empty"));
            }
            String result = aiService.getCompletion(prompt).block();
            if (result == null || result.isBlank()) {
                return ResponseEntity.ok(ApiResponse.error("AI returned no response. Check API key configuration."));
            }
            return ResponseEntity.ok(ApiResponse.success(result));
        } catch (Exception e) {
            System.err.println("AI generate-insight error: " + e.getMessage());
            return ResponseEntity.internalServerError().body(ApiResponse.error("AI service error: " + e.getMessage()));
        }
    }
}
