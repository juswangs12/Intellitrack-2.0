package com.intellitrack.controller;

import com.intellitrack.dto.ApiResponse;
import com.intellitrack.dto.StudentFeedbackDetailDto;
import com.intellitrack.dto.StudentWorkspaceDto;
import com.intellitrack.entity.AdviserFeedback;
import com.intellitrack.entity.CriterionEvaluation;
import com.intellitrack.entity.ProjectGroup;
import com.intellitrack.entity.Submission;
import com.intellitrack.entity.User;
import com.intellitrack.exception.ResourceNotFoundException;
import com.intellitrack.repository.AdviserFeedbackRepository;
import com.intellitrack.repository.SubmissionRepository;
import com.intellitrack.repository.UserRepository;
import com.intellitrack.service.StudentWorkspaceService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/api/student")
@CrossOrigin(origins = "http://localhost:3000")
public class StudentWorkspaceController {

    private final StudentWorkspaceService studentWorkspaceService;
    private final UserRepository userRepository;
    private final SubmissionRepository submissionRepository;
    private final AdviserFeedbackRepository adviserFeedbackRepository;

    public StudentWorkspaceController(
            StudentWorkspaceService studentWorkspaceService,
            UserRepository userRepository,
            SubmissionRepository submissionRepository,
            AdviserFeedbackRepository adviserFeedbackRepository) {
        this.studentWorkspaceService = studentWorkspaceService;
        this.userRepository = userRepository;
        this.submissionRepository = submissionRepository;
        this.adviserFeedbackRepository = adviserFeedbackRepository;
    }

    @GetMapping("/workspace")
    public ResponseEntity<StudentWorkspaceDto> getWorkspace(@RequestParam Long userId, Authentication authentication) {
        System.out.println("=== getWorkspace called for userId: " + userId + " ===");
        assertStudentAccess(authentication, userId);
        try {
            StudentWorkspaceDto dto = studentWorkspaceService.build(userId);
            System.out.println("=== getWorkspace successfully built ===");
            return ResponseEntity.ok(dto);
        } catch (Exception e) {
            System.err.println("=== ERROR in getWorkspace ===");
            e.printStackTrace();
            throw e;
        }
    }

    @GetMapping("/feedback/submission/{submissionId}")
    public ResponseEntity<ApiResponse<StudentFeedbackDetailDto>> getSubmissionFeedback(
            @PathVariable Long submissionId,
            @RequestParam Long userId,
            Authentication authentication) {

        System.out.println("=== getSubmissionFeedback called for submissionId: " + submissionId + ", userId: " + userId + " ===");

        assertStudentAccess(authentication, userId);

        try {
            User student = userRepository.findById(userId)
                    .orElseThrow(() -> new ResourceNotFoundException("User not found"));

            ProjectGroup group = student.getGroup();
            if (group == null) {
                System.out.println("Student has no group, returning null feedback");
                return ResponseEntity.ok(ApiResponse.success(null));
            }

            Submission submission = submissionRepository.findById(submissionId)
                    .orElseThrow(() -> new ResourceNotFoundException("Submission not found"));

            if (submission.getGroup() == null || !group.getId().equals(submission.getGroup().getId())) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Not allowed");
            }

            AdviserFeedback feedback = adviserFeedbackRepository.findBySubmissionId(submissionId).orElse(null);
            if (feedback == null) {
                System.out.println("No feedback found, returning null");
                return ResponseEntity.ok(ApiResponse.success(null));
            }

            String evaluatorName = feedback.getAdviser() == null
                    ? "Adviser"
                    : (feedback.getAdviser().getFirstName() + " " + feedback.getAdviser().getLastName()).trim();

            List<StudentFeedbackDetailDto.Criterion> criteria = feedback.getCriterionEvaluations() == null
                    ? List.of()
                    : feedback.getCriterionEvaluations().stream()
                    .map(this::toCriterionDto)
                    .toList();

            StudentFeedbackDetailDto dto = new StudentFeedbackDetailDto(
                    submissionId,
                    submission.getStatus() == null ? null : submission.getStatus().name(),
                    feedback.getTotalScore(),
                    feedback.getGeneralComments(),
                    feedback.getEvaluatedAt(),
                    evaluatorName,
                    criteria);

            System.out.println("=== getSubmissionFeedback returning dto ===");
            return ResponseEntity.ok(ApiResponse.success(dto));
        } catch (Exception e) {
            System.err.println("=== ERROR in getSubmissionFeedback ===");
            e.printStackTrace();
            throw e;
        }
    }

    private StudentFeedbackDetailDto.Criterion toCriterionDto(CriterionEvaluation evaluation) {
        var criterion = evaluation.getCriterion();
        return new StudentFeedbackDetailDto.Criterion(
                criterion == null ? null : criterion.getId(),
                criterion == null ? null : criterion.getName(),
                evaluation.getScore(),
                criterion == null ? null : criterion.getMaxPoints(),
                criterion == null ? null : criterion.getWeight(),
                evaluation.getComments());
    }

    private void assertStudentAccess(Authentication authentication, Long userId) {
        if (authentication == null || authentication.getPrincipal() == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Unauthorized");
        }

        Long principalId;
        try {
            principalId = Long.valueOf(authentication.getPrincipal().toString());
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Unauthorized");
        }

        boolean isStudent = authentication.getAuthorities().stream()
                .anyMatch(a -> "ROLE_student".equals(a.getAuthority()));

        if (!isStudent || !principalId.equals(userId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Forbidden");
        }
    }
}
