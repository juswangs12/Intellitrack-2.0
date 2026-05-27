package com.intellitrack.controller;

import com.intellitrack.dto.ApiResponse;
import com.intellitrack.dto.StudentFeedbackDetailDto;
import com.intellitrack.dto.StudentWorkspaceDto;
import com.intellitrack.entity.AdviserFeedback;
import com.intellitrack.entity.CriterionEvaluation;
import com.intellitrack.entity.ProjectGroup;
import com.intellitrack.entity.StudentEnrollment;
import com.intellitrack.entity.Submission;
import com.intellitrack.entity.User;
import com.intellitrack.exception.ResourceNotFoundException;
import com.intellitrack.repository.AdviserFeedbackRepository;
import com.intellitrack.repository.StudentEnrollmentRepository;
import com.intellitrack.repository.SubmissionRepository;
import com.intellitrack.repository.UserRepository;
import com.intellitrack.service.AiService;
import com.intellitrack.service.StudentWorkspaceService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

@SuppressWarnings("null")
@RestController
@RequestMapping("/api/student")
public class StudentWorkspaceController {

    private static final Logger log = LoggerFactory.getLogger(StudentWorkspaceController.class);
    private static final long AI_CACHE_TTL_SECONDS = 300;
    private static final long AI_MIN_COOLDOWN_SECONDS = 60;

    private record CachedStudentInsight(String insight, Instant cachedAt, Instant lastGeneratedAt) {
    }

    private final ConcurrentHashMap<Long, CachedStudentInsight> studentInsightCache = new ConcurrentHashMap<>();

    private final StudentWorkspaceService studentWorkspaceService;
    private final UserRepository userRepository;
    private final SubmissionRepository submissionRepository;
    private final AdviserFeedbackRepository adviserFeedbackRepository;
    private final StudentEnrollmentRepository studentEnrollmentRepository;
    private final AiService aiService;

    public StudentWorkspaceController(
            StudentWorkspaceService studentWorkspaceService,
            UserRepository userRepository,
            SubmissionRepository submissionRepository,
            AdviserFeedbackRepository adviserFeedbackRepository,
            StudentEnrollmentRepository studentEnrollmentRepository,
            AiService aiService) {
        this.studentWorkspaceService = studentWorkspaceService;
        this.userRepository = userRepository;
        this.submissionRepository = submissionRepository;
        this.adviserFeedbackRepository = adviserFeedbackRepository;
        this.studentEnrollmentRepository = studentEnrollmentRepository;
        this.aiService = aiService;
    }

    @GetMapping("/workspace")
    public ResponseEntity<StudentWorkspaceDto> getWorkspace(Authentication authentication) {
        System.out.println("=== getWorkspace called ===");

        Long userId = getUserIdFromAuthentication(authentication);
        System.out.println("  Authenticated userId: " + userId);

        User student = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        try {
            StudentWorkspaceDto dto = studentWorkspaceService.build(student);
            System.out.println("=== getWorkspace successfully built ===");
            return ResponseEntity.ok(dto);
        } catch (Exception e) {
            System.err.println("=== ERROR in getWorkspace ===");
            e.printStackTrace();
            throw e;
        }
    }

    /**
     * Generates an AI summary for the student's deliverables only (EXPLICIT REQUEST ONLY).
     */
    @PostMapping("/workspace/generate-ai-insight")
    public ResponseEntity<ApiResponse<String>> generateStudentAiInsight(Authentication authentication) {
        Long userId = getUserIdFromAuthentication(authentication);
        User student = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        
        CachedStudentInsight cached = studentInsightCache.get(userId);
        
        if (cached != null && cached.lastGeneratedAt() != null
                && Instant.now().isBefore(cached.lastGeneratedAt().plusSeconds(AI_MIN_COOLDOWN_SECONDS))) {
            log.info("=== USING RECENT AI ANALYSIS (Cooldown active) - Student {} ===", userId);
            return ResponseEntity.ok(ApiResponse.success(cached.insight()));
        }
        
        try {
            log.info("=== GENERATING NEW AI ANALYSIS (EXPLICIT REQUEST) - Student {} ===", userId);
            
            String prompt = studentWorkspaceService.buildAiPromptForStudent(student);
            if (prompt == null) {
                return ResponseEntity.ok(ApiResponse.success("No deliverables to analyze yet."));
            }
            
            String systemPrompt = """
                    You are an AI analytics engine for a Capstone Submission Tracking System (IntelliTrack). 
 
                    This is STRICTLY for ONE student only. 
 
                    STUDENT AI INSIGHTS OUTPUT FORMAT:
 
                    ### Student: {Full Name} 
 
                    ### Submission Status Overview 
                    For EACH deliverable: 
 
                    - {Deliverable Name} 
                      - Status: ON TIME / LATE / MISSING 
                      - Deadline: {date} 
                      - Days Remaining: {if upcoming} 
                      - Days Overdue: {if late} 
                      - Submission Date: {if submitted} 
 
                    --- 
 
                    ### Summary View 
                    - Total Deliverables: {number} 
                    - On Time: {count} 
                    - Late: {count} 
                    - Missing: {count} 
 
                    --- 
 
                    ### AI Insight 
                    - 3–5 bullet points ONLY: 
                      - focus on deadlines 
                      - missing work 
                      - risk warning 
                      - simple improvement advice 
 
                    DO NOT write paragraphs. 
                    DO NOT include group-level analytics. 
                    DO NOT include other students.
 
                    GENERAL RULES (VERY IMPORTANT):
                    - Use FULL student names (never first name only, never partial names)
                    - Be factual and structured based on provided data only
                    - Do NOT hallucinate missing data
                    - Always classify submissions as: ON TIME, LATE, MISSING
                    - Always mention deliverable names explicitly
                    - Keep language professional and analytical
                    - Avoid storytelling or vague summaries
                    - NO emojis
                    - NO fluff
                    - NO motivational phrases
                    - NO vague statements like "doing well" without data
 
                    Return ONLY the formatted output. No extra explanation.
                    """;

            
            String insight = aiService.getCompletion(prompt, systemPrompt).block();
            
            if (insight != null && !insight.isBlank()) {
                studentInsightCache.put(userId, new CachedStudentInsight(insight, Instant.now(), Instant.now()));
                log.info("=== AI ANALYSIS SAVED - Student {} ===", userId);
                return ResponseEntity.ok(ApiResponse.success(insight));
            } else {
                if (cached != null) {
                    log.info("=== USING FALLBACK CACHED AI ANALYSIS - Student {} ===", userId);
                    return ResponseEntity.ok(ApiResponse.success(cached.insight()));
                }
                return ResponseEntity.ok(ApiResponse.success("AI summary is not available at this time."));
            }
        } catch (Exception e) {
            log.error("=== AI RATE LIMIT HIT - Student {}: {} ===", userId, e.getMessage());
            if (cached != null) {
                log.info("=== FALLBACK TO CACHED AI ANALYSIS - Student {} ===", userId);
                return ResponseEntity.ok(ApiResponse.success(cached.insight()));
            }
            return ResponseEntity.ok(ApiResponse.success("AI summary is temporarily unavailable due to rate limits."));
        }
    }

    private Long getUserIdFromAuthentication(Authentication authentication) {
        if (authentication == null || authentication.getPrincipal() == null) {
            throw new org.springframework.web.server.ResponseStatusException(
                    org.springframework.http.HttpStatus.UNAUTHORIZED, "Unauthorized");
        }

        try {
            return Long.valueOf(authentication.getPrincipal().toString());
        } catch (NumberFormatException e) {
            throw new org.springframework.web.server.ResponseStatusException(
                    org.springframework.http.HttpStatus.UNAUTHORIZED, "Invalid principal");
        }
    }

    @GetMapping("/feedback/submission/{submissionId}")
    @Transactional(readOnly = true)
    public ResponseEntity<ApiResponse<StudentFeedbackDetailDto>> getSubmissionFeedback(
            @PathVariable Long submissionId,
            Authentication authentication) {

        System.out.println("=== getSubmissionFeedback called for submissionId: " + submissionId + " ===");

        Long userId = getUserIdFromAuthentication(authentication);
        System.out.println("  Authenticated userId: " + userId);

        try {
            userRepository.findById(userId)
                    .orElseThrow(() -> new ResourceNotFoundException("User not found"));

            // Find group from StudentEnrollment (only source of truth)
            ProjectGroup group = null;
            List<StudentEnrollment> enrollments = studentEnrollmentRepository.findByStudent_IdWithAllRelations(userId);
            if (!enrollments.isEmpty() && !enrollments.get(0).getGroups().isEmpty()) {
                group = enrollments.get(0).getGroups().get(0);
            }

            if (group == null) {
                System.out.println("Student has no group via enrollment, returning null feedback");
                return ResponseEntity.ok(ApiResponse.success(null));
            }

            Submission submission = submissionRepository.findById(submissionId).orElse(null);
            if (submission == null) {
                System.out.println("Submission not found for id: " + submissionId);
                return ResponseEntity.ok(ApiResponse.success(null));
            }

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

    @SuppressWarnings("unused")
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
                .anyMatch(a -> "ROLE_STUDENT".equals(a.getAuthority()));

        if (!isStudent || !principalId.equals(userId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Forbidden");
        }
    }
}
