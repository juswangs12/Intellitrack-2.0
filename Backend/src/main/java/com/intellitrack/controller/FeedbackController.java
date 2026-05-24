package com.intellitrack.controller;

import com.intellitrack.dto.ApiResponse;
import com.intellitrack.entity.AdviserFeedback;
import com.intellitrack.repository.AdviserFeedbackRepository;
import com.intellitrack.service.FeedbackService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.Map;

@SuppressWarnings("null")
@RestController
@RequestMapping("/api/feedback")
@CrossOrigin(origins = "http://localhost:3000")
@Transactional(readOnly = true)
public class FeedbackController {

    @Autowired
    private FeedbackService feedbackService;

    @Autowired
    private AdviserFeedbackRepository adviserFeedbackRepository;

    @Transactional
    @PostMapping("/comment/{submissionId}")
    public ResponseEntity<ApiResponse<Map<String, Object>>> addComment(
            @PathVariable Long submissionId,
            @RequestParam Long adviserId,
            @RequestBody Map<String, Object> payload) {

        String comments = (String) payload.get("comments");
        AdviserFeedback feedback = feedbackService.addComment(submissionId, adviserId, comments);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("id", feedback.getId());
        result.put("submissionId", submissionId);
        result.put("generalComments", feedback.getGeneralComments());
        result.put("evaluatedAt", feedback.getEvaluatedAt() != null ? feedback.getEvaluatedAt().toString() : null);
        result.put("adviserId", adviserId);

        return ResponseEntity.ok(ApiResponse.success("Comment saved successfully", result));
    }

    @GetMapping("/submission/{submissionId}")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getFeedbackForSubmission(
            @PathVariable Long submissionId) {
        AdviserFeedback feedback = adviserFeedbackRepository.findBySubmissionId(submissionId)
                .orElse(null);

        if (feedback == null) {
            return ResponseEntity.ok(ApiResponse.success(null));
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("id", feedback.getId());
        result.put("submissionId", submissionId);
        result.put("generalComments", feedback.getGeneralComments());
        result.put("evaluatedAt", feedback.getEvaluatedAt() != null ? feedback.getEvaluatedAt().toString() : null);
        if (feedback.getAdviser() != null) {
            result.put("adviserName", feedback.getAdviser().getFirstName() + " " + feedback.getAdviser().getLastName());
        }

        return ResponseEntity.ok(ApiResponse.success(result));
    }
}
