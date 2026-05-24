package com.intellitrack.controller;

import com.intellitrack.dto.ApiResponse;
import com.intellitrack.dto.FeedbackResultDto;
import com.intellitrack.dto.RubricCriterionDto;
import com.intellitrack.dto.RubricDto;
import com.intellitrack.dto.StudentFeedbackDetailDto;
import com.intellitrack.entity.AdviserFeedback;
import com.intellitrack.entity.CriterionEvaluation;
import com.intellitrack.entity.Rubric;
import com.intellitrack.entity.RubricCriterion;
import com.intellitrack.entity.SubmissionStatus;
import com.intellitrack.exception.ResourceNotFoundException;
import com.intellitrack.repository.AdviserFeedbackRepository;
import com.intellitrack.repository.RubricCriterionRepository;
import com.intellitrack.service.FeedbackService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@SuppressWarnings({ "null", "unchecked" })
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
    public ResponseEntity<ApiResponse<AdviserFeedback>> addComment(
            @PathVariable Long submissionId,
            @RequestParam Long adviserId,
            @RequestBody Map<String, Object> payload) {

        String comments = (String) payload.get("comments");
        AdviserFeedback feedback = feedbackService.addComment(submissionId, adviserId, comments);

        return ResponseEntity.ok(ApiResponse.success("Comment saved successfully", feedback));
    }

    @GetMapping("/submission/{submissionId}")
    public ResponseEntity<ApiResponse<AdviserFeedback>> getFeedbackForSubmission(
            @PathVariable Long submissionId) {
        AdviserFeedback feedback = adviserFeedbackRepository.findBySubmissionId(submissionId)
                .orElse(null);

        if (feedback == null) {
            return ResponseEntity.ok(ApiResponse.success(null));
        }

        return ResponseEntity.ok(ApiResponse.success(feedback));
    }
}
