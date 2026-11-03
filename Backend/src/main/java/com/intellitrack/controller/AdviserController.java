package com.intellitrack.controller;

import com.intellitrack.dto.ApiResponse;
import com.intellitrack.dto.DeadlineCardDto;
import com.intellitrack.dto.ProjectGroupDTO;
import com.intellitrack.dto.ReminderDto;
import com.intellitrack.dto.StudentWorkspaceDto;
import com.intellitrack.entity.Deliverable;
import com.intellitrack.entity.ProjectGroup;
import com.intellitrack.entity.StudentEnrollment;
import com.intellitrack.entity.Submission;
import com.intellitrack.entity.SubmissionStatus;
import com.intellitrack.repository.DeliverableRepository;
import com.intellitrack.repository.ProjectGroupRepository;
import com.intellitrack.repository.SubmissionRepository;
import com.intellitrack.service.AiService;
import com.intellitrack.service.DeadlineMonitoringService;
import com.intellitrack.service.StatusEvaluationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@SuppressWarnings("null")
@RestController
@RequestMapping("/api/adviser")
@Transactional(readOnly = true)
public class AdviserController {

    private static final Logger log = LoggerFactory.getLogger(AdviserController.class);
    private static final long AI_CACHE_TTL_SECONDS = 300;
    private static final long AI_MIN_COOLDOWN_SECONDS = 60;

    private record CachedGroupInsight(String insight, Instant cachedAt, Instant lastGeneratedAt) {
    }

    private final ConcurrentHashMap<Long, CachedGroupInsight> groupInsightCache = new ConcurrentHashMap<>();

    @Autowired
    private ProjectGroupRepository projectGroupRepository;

    @Autowired
    private DeliverableRepository deliverableRepository;

    @Autowired
    private SubmissionRepository submissionRepository;

    @Autowired
    private AiService aiService;

    @Autowired
    private DeadlineMonitoringService deadlineMonitoringService;

    @Autowired
    private StatusEvaluationService statusEvaluationService;

    @GetMapping("/{adviserId}/groups")
    public ResponseEntity<ApiResponse<List<ProjectGroupDTO>>> getAdviserGroups(@PathVariable Long adviserId) {
        List<ProjectGroup> groups = projectGroupRepository.findByAdviserId(adviserId);
        List<ProjectGroupDTO> dtos = groups.stream().map(ProjectGroupDTO::new).collect(Collectors.toList());
        return ResponseEntity.ok(ApiResponse.success(dtos));
    }

    @GetMapping("/groups/{groupId}")
    public ResponseEntity<ApiResponse<ProjectGroupDTO>> getGroupDetails(@PathVariable Long groupId) {
        ProjectGroup group = projectGroupRepository.findById(groupId)
                .orElseThrow(() -> new com.intellitrack.exception.ResourceNotFoundException("Group not found"));
        return ResponseEntity.ok(ApiResponse.success(new ProjectGroupDTO(group)));
    }

    @GetMapping("/groups/{groupId}/deliverables")
    public ResponseEntity<ApiResponse<List<Deliverable>>> getGroupDeliverables(@PathVariable Long groupId) {
        ProjectGroup group = projectGroupRepository.findById(groupId)
                .orElseThrow(() -> new com.intellitrack.exception.ResourceNotFoundException("Group not found"));
        
        System.out.println("=== DEBUG GROUP DELIVERABLES ===");
        System.out.println("Group ID: " + groupId);
        System.out.println("Group Code: " + group.getCode());
        System.out.println("Group Subject: " + (group.getSubject() != null ? group.getSubject().getId() + " - " + group.getSubject().getName() : "null"));
        
        List<Deliverable> deliverables = deliverableRepository.findActiveWithSubjectAndSection();
        
        System.out.println("Found " + deliverables.size() + " deliverables");
        for (Deliverable d : deliverables) {
            System.out.println("  - " + d.getName() + " (subject: " + (d.getSubject() != null ? d.getSubject().getName() : "none") + ")");
        }
        
        return ResponseEntity.ok(ApiResponse.success(deliverables));
    }

    @PostMapping("/groups/{groupId}/ai-summary")
    public ResponseEntity<ApiResponse<String>> generateGroupAiSummary(@PathVariable Long groupId) {
        ProjectGroup group = projectGroupRepository.findByIdWithStudentsAndStudent(groupId)
                .orElseThrow(() -> new com.intellitrack.exception.ResourceNotFoundException("Group not found"));

        CachedGroupInsight cached = groupInsightCache.get(groupId);

        if (cached != null && cached.lastGeneratedAt() != null
                && Instant.now().isBefore(cached.lastGeneratedAt().plusSeconds(AI_MIN_COOLDOWN_SECONDS))) {
            log.info("=== USING RECENT AI ANALYSIS (Cooldown active) - Group {} ===", groupId);
            return ResponseEntity.ok(ApiResponse.success(cached.insight()));
        }

        try {
            log.info("=== GENERATING NEW AI ANALYSIS (EXPLICIT REQUEST) - Group {} ===", groupId);

            List<Deliverable> deliverables = deliverableRepository.findActiveWithSubjectAndSection();
            Map<Long, Submission> submissionsByDeliverableId = new HashMap<>();
            List<Submission> submissions = submissionRepository.findByGroupId(groupId);
            for (Submission submission : submissions) {
                if (submission != null && submission.getDeliverable() != null
                        && submission.getDeliverable().getId() != null) {
                    submissionsByDeliverableId.put(submission.getDeliverable().getId(), submission);
                }
            }

            List<StudentWorkspaceDto.DeliverableRow> rows = new ArrayList<>();
            for (Deliverable deliverable : deliverables) {
                Submission submission = submissionsByDeliverableId.get(deliverable.getId());
                SubmissionStatus status = statusEvaluationService.computeStatus(submission, deliverable.getDueAt());

                rows.add(new StudentWorkspaceDto.DeliverableRow(
                        deliverable.getId(),
                        deliverable.getName(),
                        deliverable.getStage(),
                        status.name(),
                        deliverable.getDueAt(),
                        0L,
                        submission != null ? submission.getSubmittedAt() : null,
                        submission != null ? submission.getId() : null,
                        submission != null ? submission.getVersionNumber() : null,
                        submission != null && submission.getAiSummary() != null
                                && !submission.getAiSummary().isBlank()));
            }

            rows.sort(Comparator
                    .comparing((StudentWorkspaceDto.DeliverableRow row) -> row.dueAt() == null)
                    .thenComparing(row -> row.dueAt() == null ? LocalDateTime.MAX : row.dueAt()));

            List<DeadlineCardDto> upcomingDeadlines = deadlineMonitoringService.getActiveDeadlines(groupId)
                    .stream()
                    .sorted(Comparator.comparing(DeadlineCardDto::hoursRemaining,
                            Comparator.nullsLast(Comparator.naturalOrder())))
                    .limit(6)
                    .toList();

            StringBuilder promptBuilder = new StringBuilder();
            promptBuilder.append("You are an AI analytics engine for a Capstone Submission Tracking System (IntelliTrack).\n\n");
            promptBuilder.append("Your job is to generate STRICTLY structured, clear, and actionable summaries for the Adviser role.\n\n");
            promptBuilder.append("You MUST follow the formatting rules exactly. Do not be creative with structure.\n\n");
            promptBuilder.append("GENERAL RULES (VERY IMPORTANT):\n");
            promptBuilder.append("- Use FULL student names (never first name only, never partial names)\n");
            promptBuilder.append("- Be factual and structured based on provided data only\n");
            promptBuilder.append("- Do NOT hallucinate missing data\n");
            promptBuilder.append("- Always classify submissions as: ON TIME, LATE, MISSING\n");
            promptBuilder.append("- Always mention deliverable names explicitly\n");
            promptBuilder.append("- Keep language professional and analytical\n");
            promptBuilder.append("- Avoid storytelling or vague summaries\n");
            promptBuilder.append("- NO emojis\n");
            promptBuilder.append("- NO fluff\n");
            promptBuilder.append("- NO motivational phrases\n");
            promptBuilder.append("- NO vague statements like \"doing well\" without data\n\n");
            promptBuilder.append("ADVISER ANALYTICS (MY GROUPS ONLY) OUTPUT FORMAT:\n\n");
            promptBuilder.append("### Group: {Group Code}\n\n");
            promptBuilder.append("- Progress Status: (Healthy / At Risk / Critical)\n\n");
            promptBuilder.append("- Submission Overview:\n");
            promptBuilder.append("  - Total Deliverables: {number}\n");
            promptBuilder.append("  - On Time: {count}\n");
            promptBuilder.append("  - Late: {count}\n");
            promptBuilder.append("  - Missing: {count}\n\n");
            promptBuilder.append("---\n\n");
            promptBuilder.append("### Student Breakdown\n\n");
            promptBuilder.append("First, list ALL group students with their details:\n");
            promptBuilder.append("- {Full Name} (ID: {Student ID}) → Status: (Linked / Not Linked)\n\n");
            promptBuilder.append("Then:\n\n");
            promptBuilder.append("#### Submitted Students\n");
            promptBuilder.append("- {Full Name}\n");
            promptBuilder.append("  - Deliverables Submitted:\n");
            promptBuilder.append("    - {Deliverable Name} → ON TIME / LATE\n\n");
            promptBuilder.append("#### Students With Issues\n");
            promptBuilder.append("- {Full Name}\n");
            promptBuilder.append("  - Missing Deliverables:\n");
            promptBuilder.append("    - {Deliverable Name}\n");
            promptBuilder.append("  - Late Deliverables:\n");
            promptBuilder.append("    - {Deliverable Name} (X days late)\n\n");
            promptBuilder.append("---\n\n");
            promptBuilder.append("### Group Insight Summary\n");
            promptBuilder.append("- 3–5 sentences max summarizing:\n");
            promptBuilder.append("  - group performance\n");
            promptBuilder.append("  - submission behavior\n");
            promptBuilder.append("  - risk level\n");
            promptBuilder.append("  - who needs attention\n\n");
            promptBuilder.append("DO NOT mention system-wide data.\n\n");
            promptBuilder.append("Return ONLY the formatted output. No extra explanation.\n\n");
            promptBuilder.append("=== GROUP INFO ===\n");
            promptBuilder.append("Group: ").append(group.getTitle() != null ? group.getTitle() : "Unknown").append("\n");
            if (group.getSubject() != null) {
                promptBuilder.append("Subject: ").append(group.getSubject().getName()).append("\n");
            }
            
            promptBuilder.append("\n=== GROUP STUDENTS ===\n");
            if (group.getStudents() != null && !group.getStudents().isEmpty()) {
                for (StudentEnrollment student : group.getStudents()) {
                    String fullName = student.getFullName() != null ? student.getFullName() : "Unknown Student";
                    String studentId = student.getStudentId() != null ? student.getStudentId() : "N/A";
                    boolean isLinked = student.getStudent() != null;
                    promptBuilder.append("- ").append(fullName)
                            .append(" (ID: ").append(studentId).append(") → ")
                            .append(isLinked ? "Linked" : "Not Linked")
                            .append("\n");
                }
            } else {
                promptBuilder.append("No students in this group.\n");
            }
            
            promptBuilder.append("\n=== DELIVERABLES ===\n");
            for (StudentWorkspaceDto.DeliverableRow row : rows) {
                promptBuilder.append("- ").append(row.deliverableName()).append(" (").append(row.stage()).append("): ").append(row.status()).append("\n");
            }

            String systemPrompt = "Follow the structured format exactly.";

            String summary = aiService.getCompletion(promptBuilder.toString(), systemPrompt).block();

            if (summary != null && !summary.isBlank()) {
                groupInsightCache.put(groupId, new CachedGroupInsight(summary, Instant.now(), Instant.now()));
                log.info("=== AI ANALYSIS SAVED - Group {} ===", groupId);
                return ResponseEntity.ok(ApiResponse.success(summary));
            } else {
                if (cached != null) {
                    log.info("=== USING FALLBACK CACHED AI ANALYSIS - Group {} ===", groupId);
                    return ResponseEntity.ok(ApiResponse.success(cached.insight()));
                }
                return ResponseEntity.ok(ApiResponse.success("AI summary is not available at this time."));
            }
        } catch (Exception e) {
            log.error("=== AI RATE LIMIT HIT - Group {}: {} ===", groupId, e.getMessage());
            if (cached != null) {
                log.info("=== FALLBACK TO CACHED AI ANALYSIS - Group {} ===", groupId);
                return ResponseEntity.ok(ApiResponse.success(cached.insight()));
            }
            return ResponseEntity.ok(ApiResponse.success("AI summary is temporarily unavailable due to rate limits."));
        }
    }
}
