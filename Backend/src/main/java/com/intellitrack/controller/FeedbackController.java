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
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/feedback")
@CrossOrigin(origins = "http://localhost:3000")
public class FeedbackController {

    @Autowired
    private FeedbackService feedbackService;

    @Autowired
    private RubricCriterionRepository rubricCriterionRepository;

    @Autowired
    private AdviserFeedbackRepository adviserFeedbackRepository;

    @PostMapping("/evaluate/{submissionId}")
    public ResponseEntity<ApiResponse<FeedbackResultDto>> evaluate(
            @PathVariable Long submissionId,
            @RequestParam Long adviserId,
            @RequestBody Map<String, Object> payload) {
        
        String comments = (String) payload.get("comments");
        SubmissionStatus status = SubmissionStatus.valueOf((String) payload.get("status"));
        
        List<Map<String, Object>> evaluationsRaw = (List<Map<String, Object>>) payload.getOrDefault("evaluations", List.of());
        List<CriterionEvaluation> evaluations = new ArrayList<>();
        for (Map<String, Object> map : evaluationsRaw) {
            Long criterionId = null;
            if (map.containsKey("criterionId")) {
                criterionId = Long.valueOf(map.get("criterionId").toString());
            } else if (map.containsKey("criterion")) {
                Object criterionObj = map.get("criterion");
                if (criterionObj instanceof Map<?, ?> criterionMap && criterionMap.get("id") != null) {
                    criterionId = Long.valueOf(criterionMap.get("id").toString());
                }
            }

            if (criterionId == null) {
                continue;
            }

            RubricCriterion criterion = rubricCriterionRepository.findById(criterionId)
                    .orElseThrow(() -> new ResourceNotFoundException("Rubric criterion not found"));

            Integer score = map.get("score") == null ? null : Integer.valueOf(map.get("score").toString());
            if (score == null) {
                continue;
            }
            if (score < 0 || score > criterion.getMaxPoints()) {
                throw new IllegalArgumentException("Score out of range for criterion: " + criterion.getName());
            }

            CriterionEvaluation evaluation = new CriterionEvaluation();
            evaluation.setCriterion(criterion);
            evaluation.setScore(score);
            evaluation.setComments(map.get("comments") == null ? null : map.get("comments").toString());
            evaluations.add(evaluation);
        }

        AdviserFeedback feedback = feedbackService.evaluateSubmission(submissionId, adviserId, comments, status, evaluations);
        FeedbackResultDto dto = new FeedbackResultDto(
                feedback.getSubmission() == null ? submissionId : feedback.getSubmission().getId(),
                status.name(),
                feedback.getTotalScore(),
                feedback.getEvaluatedAt());

        return ResponseEntity.ok(ApiResponse.success("Evaluation saved successfully", dto));
    }

    @GetMapping("/submission/{submissionId}")
    public ResponseEntity<ApiResponse<StudentFeedbackDetailDto>> getFeedbackForSubmission(@PathVariable Long submissionId) {
        AdviserFeedback feedback = adviserFeedbackRepository.findBySubmissionId(submissionId)
                .orElse(null);
        
        if (feedback == null) {
            return ResponseEntity.ok(ApiResponse.success(null));
        }

        List<StudentFeedbackDetailDto.Criterion> criteria = feedback.getCriterionEvaluations().stream()
                .map(e -> new StudentFeedbackDetailDto.Criterion(
                        e.getCriterion().getId(),
                        e.getCriterion().getName(),
                        e.getScore(),
                        e.getCriterion().getMaxPoints(),
                        e.getCriterion().getWeight(),
                        e.getComments()))
                .collect(Collectors.toList());

        String evaluatorName = feedback.getAdviser() != null 
                ? feedback.getAdviser().getFirstName() + " " + feedback.getAdviser().getLastName() 
                : "Unknown";

        StudentFeedbackDetailDto dto = new StudentFeedbackDetailDto(
                feedback.getSubmission() != null ? feedback.getSubmission().getId() : submissionId,
                feedback.getSubmission() != null && feedback.getSubmission().getStatus() != null 
                        ? feedback.getSubmission().getStatus().name() 
                        : null,
                feedback.getTotalScore(),
                feedback.getGeneralComments(),
                feedback.getEvaluatedAt(),
                evaluatorName,
                criteria);

        return ResponseEntity.ok(ApiResponse.success(dto));
    }

    @GetMapping("/group/{groupId}/history")
    public ResponseEntity<ApiResponse<List<StudentFeedbackDetailDto>>> getFeedbackHistoryForGroup(@PathVariable Long groupId) {
        List<AdviserFeedback> feedbacks = adviserFeedbackRepository.findBySubmission_Group_IdOrderByEvaluatedAtDesc(groupId);
        
        List<StudentFeedbackDetailDto> dtos = feedbacks.stream()
                .map(feedback -> {
                    List<StudentFeedbackDetailDto.Criterion> criteria = feedback.getCriterionEvaluations().stream()
                            .map(e -> new StudentFeedbackDetailDto.Criterion(
                                    e.getCriterion().getId(),
                                    e.getCriterion().getName(),
                                    e.getScore(),
                                    e.getCriterion().getMaxPoints(),
                                    e.getCriterion().getWeight(),
                                    e.getComments()))
                            .collect(Collectors.toList());

                    String evaluatorName = feedback.getAdviser() != null 
                            ? feedback.getAdviser().getFirstName() + " " + feedback.getAdviser().getLastName() 
                            : "Unknown";

                    return new StudentFeedbackDetailDto(
                            feedback.getSubmission() != null ? feedback.getSubmission().getId() : null,
                            feedback.getSubmission() != null && feedback.getSubmission().getStatus() != null 
                                    ? feedback.getSubmission().getStatus().name() 
                                    : null,
                            feedback.getTotalScore(),
                            feedback.getGeneralComments(),
                            feedback.getEvaluatedAt(),
                            evaluatorName,
                            criteria);
                })
                .collect(Collectors.toList());

        return ResponseEntity.ok(ApiResponse.success(dtos));
    }

    @GetMapping("/rubrics")
    public ResponseEntity<ApiResponse<List<RubricDto>>> getRubrics() {
        List<RubricDto> dtos = feedbackService.getAllRubrics().stream()
            .map(this::toRubricDto)
            .collect(Collectors.toList());
        return ResponseEntity.ok(ApiResponse.success(dtos));
    }

    @PostMapping("/rubrics")
    public ResponseEntity<ApiResponse<Rubric>> createRubric(@RequestBody Rubric rubric) {
        return ResponseEntity.ok(ApiResponse.success("Rubric created", feedbackService.createRubric(rubric)));
    }

    private RubricDto toRubricDto(Rubric rubric) {
        List<RubricCriterionDto> criteria = rubric.getCriteria() == null
                ? List.of()
                : rubric.getCriteria().stream()
                    .map(c -> new RubricCriterionDto(
                        c.getId(),
                        c.getName(),
                        c.getDescription(),
                        c.getMaxPoints(),
                        c.getWeight()))
                    .toList();

        return new RubricDto(
                rubric.getId(),
                rubric.getTitle(),
                rubric.getDescription(),
                criteria);
    }
}
