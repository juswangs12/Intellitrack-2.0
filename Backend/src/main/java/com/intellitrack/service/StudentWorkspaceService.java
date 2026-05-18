package com.intellitrack.service;

import com.intellitrack.dto.DeadlineCardDto;
import com.intellitrack.dto.ReminderDto;
import com.intellitrack.dto.StudentWorkspaceDto;
import com.intellitrack.entity.AdviserFeedback;
import com.intellitrack.entity.Deadline;
import com.intellitrack.entity.Deliverable;
import com.intellitrack.entity.ProjectGroup;
import com.intellitrack.entity.StudentEnrollment;
import com.intellitrack.entity.Submission;
import com.intellitrack.entity.SubmissionStatus;
import com.intellitrack.entity.User;
import com.intellitrack.exception.ResourceNotFoundException;
import com.intellitrack.repository.AdviserFeedbackRepository;
import com.intellitrack.repository.DeadlineRepository;
import com.intellitrack.repository.DeliverableRepository;
import com.intellitrack.repository.ProjectGroupRepository;
import com.intellitrack.repository.StudentEnrollmentRepository;
import com.intellitrack.repository.SubmissionRepository;
import com.intellitrack.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@Transactional(readOnly = true)
public class StudentWorkspaceService {

    private final UserRepository userRepository;
    private final ProjectGroupRepository projectGroupRepository;
    private final DeliverableRepository deliverableRepository;
    private final DeadlineRepository deadlineRepository;
    private final SubmissionRepository submissionRepository;
    private final AdviserFeedbackRepository adviserFeedbackRepository;
    private final DeadlineMonitoringService deadlineMonitoringService;
    private final StatusEvaluationService statusEvaluationService;
    private final AiService aiService;
    private final StudentEnrollmentRepository studentEnrollmentRepository;

    public StudentWorkspaceService(
            UserRepository userRepository,
            ProjectGroupRepository projectGroupRepository,
            DeliverableRepository deliverableRepository,
            DeadlineRepository deadlineRepository,
            SubmissionRepository submissionRepository,
            AdviserFeedbackRepository adviserFeedbackRepository,
            DeadlineMonitoringService deadlineMonitoringService,
            StatusEvaluationService statusEvaluationService,
            AiService aiService,
            StudentEnrollmentRepository studentEnrollmentRepository) {
        this.userRepository = userRepository;
        this.projectGroupRepository = projectGroupRepository;
        this.deliverableRepository = deliverableRepository;
        this.deadlineRepository = deadlineRepository;
        this.submissionRepository = submissionRepository;
        this.adviserFeedbackRepository = adviserFeedbackRepository;
        this.deadlineMonitoringService = deadlineMonitoringService;
        this.statusEvaluationService = statusEvaluationService;
        this.aiService = aiService;
        this.studentEnrollmentRepository = studentEnrollmentRepository;
    }

    public StudentWorkspaceDto build(Long studentUserId) {
        System.out.println("=== StudentWorkspaceService.build called for userId: " + studentUserId + " ===");
        
        try {
            // ==============================================================
            // 1. SAFELY LOAD STUDENT USER
            // ==============================================================
            User student = userRepository.findById(studentUserId).orElse(null);
            if (student == null) {
                System.err.println("  ERROR: User not found for id=" + studentUserId);
                return buildEmptyWorkspaceForError(studentUserId, "User not found");
            }
            
            System.out.println("  Student found: email=" + student.getEmail() + ", role=" + student.getRole());

            if (!"student".equalsIgnoreCase(student.getRole())) {
                System.err.println("  ERROR: User is not a student, role=" + student.getRole());
                return buildEmptyWorkspaceForError(student.getId(), 
                    (student.getFirstName() != null ? student.getFirstName() : "User") + " " + 
                    (student.getLastName() != null ? student.getLastName() : ""), 
                    "Not a student");
            }

            // ==============================================================
            // 2. SAFELY LOAD ENROLLMENTS
            // ==============================================================
            List<StudentEnrollment> enrollments = safeFindEnrollments(studentUserId);
            System.out.println("  Found " + enrollments.size() + " enrollments");

            // ==============================================================
            // 3. SAFELY GET GROUP FROM ENROLLMENTS
            // ==============================================================
            ProjectGroup group = safeGetGroupFromEnrollments(enrollments);
            System.out.println("  Group determined: " + (group != null ? "id=" + group.getId() + ", title=" + group.getTitle() : "null"));

            // ==============================================================
            // 4. IF NO GROUP, RETURN EMPTY BUT STABLE WORKSPACE
            // ==============================================================
            if (group == null) {
                System.out.println("  No group found, returning empty workspace");
                return buildEmptyWorkspace(student, "No group assigned yet");
            }

            // ==============================================================
            // 5. SAFELY LOAD GROUP WITH FULL DATA
            // ==============================================================
            ProjectGroup hydratedGroup = projectGroupRepository.findById(group.getId()).orElse(null);
            if (hydratedGroup == null) {
                hydratedGroup = group; // Fallback to the group we already have
            }
            System.out.println("  Hydrated group: " + hydratedGroup.getTitle());

            // ==============================================================
            // 6. SAFELY LOAD DELIVERABLES, DEADLINES, SUBMISSIONS
            // ==============================================================
            List<Deliverable> deliverables = safeGetDeliverables();
            Map<Long, Submission> submissionsByDeliverableId = safeGetSubmissionsMap(hydratedGroup);
            System.out.println("  Deliverables=" + deliverables.size() + 
                             ", Submissions=" + submissionsByDeliverableId.size());

            // ==============================================================
            // 7. SAFELY BUILD DELIVERABLE ROWS (USING UNIFIED DELIVERABLE)
            // ==============================================================
            List<StudentWorkspaceDto.DeliverableRow> rows = safeBuildDeliverableRows(
                deliverables, submissionsByDeliverableId);
            System.out.println("  Built " + rows.size() + " deliverable rows");

            // ==============================================================
            // 8. SAFELY CALCULATE SUBMISSION OVERVIEW
            // ==============================================================
            StudentWorkspaceDto.SubmissionOverview overview = safeCalculateOverview(rows, deliverables.size());

            // ==============================================================
            // 9. SAFELY GET DEADLINE CARDS, REMINDERS, FEEDBACK, TIMELINE
            // ==============================================================
            List<DeadlineCardDto> deadlineCards = safeGetDeadlineCards(hydratedGroup);
            List<DeadlineCardDto> upcomingDeadlines = safeGetUpcomingDeadlines(deadlineCards);
            List<ReminderDto> reminders = safeGetReminders(studentUserId);
            List<StudentWorkspaceDto.FeedbackItem> recentFeedback = safeGetRecentFeedback(hydratedGroup);
            List<StudentWorkspaceDto.TimelineItem> timeline = safeBuildTimeline(rows, recentFeedback);

            // ==============================================================
            // 10. SAFELY GET ADVISER NAME AND AI INSIGHT
            // ==============================================================
            String adviserName = safeGetAdviserName(hydratedGroup);
            String aiInsight = safeGetAiInsight(hydratedGroup, rows, upcomingDeadlines, reminders);

            // ==============================================================
            // 11. BUILD AND RETURN FINAL DTO
            // ==============================================================
            System.out.println("=== StudentWorkspaceService.build complete successfully ===");
            
            return new StudentWorkspaceDto(
                new StudentWorkspaceDto.Header(
                    student.getId(),
                    safeGetStudentName(student),
                    hydratedGroup.getId(),
                    safeGetGroupCode(hydratedGroup),
                    safeGetGroupTitle(hydratedGroup),
                    safeGetAdviserId(hydratedGroup),
                    adviserName),
                overview,
                rows,
                upcomingDeadlines,
                reminders,
                aiInsight,
                recentFeedback,
                timeline);
                
        } catch (Exception e) {
            System.err.println("=== CRITICAL ERROR in StudentWorkspaceService.build ===");
            e.printStackTrace();
            return buildEmptyWorkspaceForError(studentUserId, "System error loading workspace");
        }
    }

    // ==============================================================
    // SAFETY HELPER METHODS - NO NULLPOINTEREXCEPTIONS!
    // ==============================================================

    private List<StudentEnrollment> safeFindEnrollments(Long userId) {
        try {
            return studentEnrollmentRepository.findByStudent_IdWithAllRelations(userId);
        } catch (Exception e) {
            System.err.println("  Error loading enrollments: " + e.getMessage());
            return List.of();
        }
    }

    private ProjectGroup safeGetGroupFromEnrollments(List<StudentEnrollment> enrollments) {
        if (enrollments == null || enrollments.isEmpty()) return null;
        
        StudentEnrollment firstEnrollment = enrollments.get(0);
        if (firstEnrollment == null) return null;
        
        List<ProjectGroup> groups = firstEnrollment.getGroups();
        if (groups == null || groups.isEmpty()) return null;
        
        return groups.get(0);
    }

    private List<Deliverable> safeGetDeliverables() {
        try {
            return deliverableRepository.findByActiveTrue();
        } catch (Exception e) {
            System.err.println("  Error loading deliverables: " + e.getMessage());
            return List.of();
        }
    }

    private Map<Long, Deadline> safeGetDeadlinesMap() {
        Map<Long, Deadline> map = new HashMap<>();
        try {
            List<Deadline> deadlines = deadlineRepository.findAll();
            for (Deadline deadline : deadlines) {
                if (deadline != null && deadline.getDeliverable() != null && deadline.getDeliverable().getId() != null) {
                    map.put(deadline.getDeliverable().getId(), deadline);
                }
            }
        } catch (Exception e) {
            System.err.println("  Error loading deadlines: " + e.getMessage());
        }
        return map;
    }

    private Map<Long, Submission> safeGetSubmissionsMap(ProjectGroup group) {
        Map<Long, Submission> map = new HashMap<>();
        if (group == null || group.getId() == null) return map;
        
        try {
            List<Submission> submissions = submissionRepository.findByGroupId(group.getId());
            for (Submission submission : submissions) {
                if (submission != null && submission.getDeliverable() != null && submission.getDeliverable().getId() != null) {
                    map.put(submission.getDeliverable().getId(), submission);
                }
            }
        } catch (Exception e) {
            System.err.println("  Error loading submissions: " + e.getMessage());
        }
        return map;
    }

    private List<StudentWorkspaceDto.DeliverableRow> safeBuildDeliverableRows(
            List<Deliverable> deliverables, 
            Map<Long, Submission> submissionsByDeliverableId) {
        
        List<StudentWorkspaceDto.DeliverableRow> rows = new ArrayList<>();
        
        if (deliverables == null) return rows;
        
        for (Deliverable deliverable : deliverables) {
            if (deliverable == null) continue;
            
            try {
                Submission submission = submissionsByDeliverableId.getOrDefault(deliverable.getId(), null);
                SubmissionStatus status = safeComputeStatus(submission, deliverable);
                
                Long hoursRemaining = null;
                if (deliverable.getDueAt() != null) {
                    try {
                        hoursRemaining = java.time.Duration.between(LocalDateTime.now(), deliverable.getDueAt()).toHours();
                    } catch (Exception e) {
                        hoursRemaining = null;
                    }
                }

                rows.add(new StudentWorkspaceDto.DeliverableRow(
                    deliverable.getId(),
                    safeGetDeliverableName(deliverable),
                    safeGetDeliverableStage(deliverable),
                    status.name(),
                    deliverable.getDueAt(),
                    hoursRemaining,
                    submission == null ? 0 : safeGetRevisionCount(submission),
                    submission == null ? null : submission.getSubmittedAt(),
                    submission == null ? null : submission.getId(),
                    submission == null ? null : submission.getVersionNumber(),
                    submission != null && submission.getAiSummary() != null && !submission.getAiSummary().isBlank()));
            } catch (Exception e) {
                System.err.println("  Error building row for deliverable " + deliverable.getId() + ": " + e.getMessage());
            }
        }
        
        rows.sort(Comparator
            .comparing((StudentWorkspaceDto.DeliverableRow row) -> row.dueAt() == null)
            .thenComparing(row -> row.dueAt() == null ? LocalDateTime.MAX : row.dueAt()));
        
        return rows;
    }

    private SubmissionStatus safeComputeStatus(Submission submission, Deliverable deliverable) {
        try {
            Deadline compatibilityDeadline = deliverable != null ? new Deadline(deliverable) : null;
            return statusEvaluationService.computeStatus(submission, compatibilityDeadline);
        } catch (Exception e) {
            System.err.println("  Error computing status: " + e.getMessage());
            return SubmissionStatus.PENDING;
        }
    }

    private SubmissionStatus safeComputeStatus(Submission submission, Deadline deadline) {
        try {
            return statusEvaluationService.computeStatus(submission, deadline);
        } catch (Exception e) {
            System.err.println("  Error computing status: " + e.getMessage());
            return SubmissionStatus.PENDING;
        }
    }

    private StudentWorkspaceDto.SubmissionOverview safeCalculateOverview(
            List<StudentWorkspaceDto.DeliverableRow> rows, long totalDeliverables) {
        
        if (rows == null) {
            return new StudentWorkspaceDto.SubmissionOverview(
                totalDeliverables, 0, totalDeliverables, 0, 0, 0, 0, 0);
        }
        
        long approved = countByStatus(rows, SubmissionStatus.APPROVED);
        long rejected = countByStatus(rows, SubmissionStatus.REJECTED);
        long needsRevision = countByStatus(rows, SubmissionStatus.NEEDS_REVISION);
        long late = countByStatus(rows, SubmissionStatus.LATE);
        long pending = countByStatus(rows, SubmissionStatus.PENDING);
        long underReview = countByStatus(rows, SubmissionStatus.SUBMITTED) + 
                          countByStatus(rows, SubmissionStatus.UPDATED);
        long submitted = totalDeliverables - pending;
        
        return new StudentWorkspaceDto.SubmissionOverview(
            totalDeliverables, submitted, pending, late, underReview, 
            needsRevision, approved, rejected);
    }

    private long countByStatus(List<StudentWorkspaceDto.DeliverableRow> rows, SubmissionStatus status) {
        if (rows == null) return 0;
        return rows.stream()
            .filter(r -> r != null && status.name().equals(r.status()))
            .count();
    }

    private List<DeadlineCardDto> safeGetDeadlineCards(ProjectGroup group) {
        if (group == null || group.getId() == null) return List.of();
        
        try {
            return deadlineMonitoringService.getActiveDeadlines(group.getId());
        } catch (Exception e) {
            System.err.println("  Error loading deadline cards: " + e.getMessage());
            return List.of();
        }
    }

    private List<DeadlineCardDto> safeGetUpcomingDeadlines(List<DeadlineCardDto> cards) {
        if (cards == null) return List.of();
        
        return cards.stream()
            .filter(card -> card != null && card.dueAt() != null)
            .sorted(Comparator.comparing(DeadlineCardDto::hoursRemaining, 
                Comparator.nullsLast(Comparator.naturalOrder())))
            .limit(6)
            .toList();
    }

    private List<ReminderDto> safeGetReminders(Long userId) {
        try {
            List<ReminderDto> reminders = deadlineMonitoringService.getReminders(userId);
            if (reminders == null) return List.of();
            return reminders.stream().limit(6).toList();
        } catch (Exception e) {
            System.err.println("  Error loading reminders: " + e.getMessage());
            return List.of();
        }
    }

    private List<StudentWorkspaceDto.FeedbackItem> safeGetRecentFeedback(ProjectGroup group) {
        if (group == null || group.getId() == null) return List.of();
        
        try {
            List<AdviserFeedback> feedbacks = adviserFeedbackRepository
                .findBySubmission_Group_IdOrderByEvaluatedAtDesc(group.getId());
            
            if (feedbacks == null) return List.of();
            
            return feedbacks.stream()
                .limit(6)
                .map(this::safeToFeedbackItem)
                .filter(item -> item != null)
                .toList();
        } catch (Exception e) {
            System.err.println("  Error loading feedback: " + e.getMessage());
            return List.of();
        }
    }

    private StudentWorkspaceDto.FeedbackItem safeToFeedbackItem(AdviserFeedback feedback) {
        if (feedback == null) return null;
        
        try {
            Submission submission = feedback.getSubmission();
            Deliverable deliverable = submission == null ? null : submission.getDeliverable();
            
            return new StudentWorkspaceDto.FeedbackItem(
                submission == null ? null : submission.getId(),
                deliverable == null ? null : deliverable.getId(),
                deliverable == null ? null : deliverable.getName(),
                submission == null || submission.getStatus() == null ? null : submission.getStatus().name(),
                feedback.getTotalScore(),
                feedback.getGeneralComments(),
                feedback.getEvaluatedAt(),
                safeGetEvaluatorName(feedback));
        } catch (Exception e) {
            System.err.println("  Error mapping feedback: " + e.getMessage());
            return null;
        }
    }

    private String safeGetEvaluatorName(AdviserFeedback feedback) {
        if (feedback == null || feedback.getAdviser() == null) return "Adviser";
        User adviser = feedback.getAdviser();
        String firstName = adviser.getFirstName() != null ? adviser.getFirstName() : "";
        String lastName = adviser.getLastName() != null ? adviser.getLastName() : "";
        String fullName = (firstName + " " + lastName).trim();
        return fullName.isEmpty() ? "Adviser" : fullName;
    }

    private List<StudentWorkspaceDto.TimelineItem> safeBuildTimeline(
            List<StudentWorkspaceDto.DeliverableRow> rows,
            List<StudentWorkspaceDto.FeedbackItem> recentFeedback) {
        
        List<StudentWorkspaceDto.TimelineItem> items = new ArrayList<>();
        
        if (rows != null) {
            for (StudentWorkspaceDto.DeliverableRow row : rows) {
                if (row == null) continue;
                
                if (row.submittedAt() != null) {
                    items.add(new StudentWorkspaceDto.TimelineItem(
                        "SUBMISSION",
                        "Submission uploaded",
                        safeGetTimelineSubmissionDetail(row),
                        row.submittedAt(),
                        row.deliverableId(),
                        row.submissionId()));
                }
                
                if (row.dueAt() != null) {
                    items.add(new StudentWorkspaceDto.TimelineItem(
                        "DEADLINE",
                        "Deadline scheduled",
                        (row.deliverableName() != null ? row.deliverableName() : "Deliverable") + " due",
                        row.dueAt(),
                        row.deliverableId(),
                        row.submissionId()));
                }
            }
        }
        
        if (recentFeedback != null) {
            for (StudentWorkspaceDto.FeedbackItem feedback : recentFeedback) {
                if (feedback == null || feedback.evaluatedAt() == null) continue;
                
                items.add(new StudentWorkspaceDto.TimelineItem(
                    "FEEDBACK",
                    "Feedback posted",
                    safeGetTimelineFeedbackDetail(feedback),
                    feedback.evaluatedAt(),
                    feedback.deliverableId(),
                    feedback.submissionId()));
            }
        }
        
        items.sort(Comparator.comparing(
            StudentWorkspaceDto.TimelineItem::occurredAt, 
            Comparator.nullsLast(Comparator.naturalOrder())).reversed());
        
        return items.stream().limit(20).toList();
    }

    private String safeGetTimelineSubmissionDetail(StudentWorkspaceDto.DeliverableRow row) {
        String name = row.deliverableName() != null ? row.deliverableName() : "Deliverable";
        int version = row.versionNumber() != null ? row.versionNumber() : 1;
        return name + " • Version " + version;
    }

    private String safeGetTimelineFeedbackDetail(StudentWorkspaceDto.FeedbackItem feedback) {
        String name = feedback.deliverableName() != null ? feedback.deliverableName() : "Deliverable";
        String status = feedback.status() != null ? feedback.status() : "UPDATED";
        return name + " • " + status;
    }

    private String safeGetStudentName(User student) {
        if (student == null) return "Student";
        String firstName = student.getFirstName() != null ? student.getFirstName() : "";
        String lastName = student.getLastName() != null ? student.getLastName() : "";
        String fullName = (firstName + " " + lastName).trim();
        return fullName.isEmpty() ? "Student" : fullName;
    }

    private String safeGetDeliverableName(Deliverable d) {
        return d != null && d.getName() != null ? d.getName() : "Deliverable";
    }

    private String safeGetDeliverableStage(Deliverable d) {
        return d != null && d.getStage() != null ? d.getStage() : "";
    }

    private Integer safeGetRevisionCount(Submission s) {
        return s != null && s.getRevisionCount() != null ? s.getRevisionCount() : 0;
    }

    private String safeGetGroupCode(ProjectGroup g) {
        return g != null ? g.getCode() : null;
    }

    private String safeGetGroupTitle(ProjectGroup g) {
        return g != null ? g.getTitle() : null;
    }

    private Long safeGetAdviserId(ProjectGroup g) {
        return g != null && g.getAdviser() != null ? g.getAdviser().getId() : null;
    }

    private String safeGetAdviserName(ProjectGroup g) {
        if (g == null || g.getAdviser() == null) return null;
        User adviser = g.getAdviser();
        String firstName = adviser.getFirstName() != null ? adviser.getFirstName() : "";
        String lastName = adviser.getLastName() != null ? adviser.getLastName() : "";
        String fullName = (firstName + " " + lastName).trim();
        return fullName.isEmpty() ? null : fullName;
    }

    private String safeGetAiInsight(ProjectGroup group, List<StudentWorkspaceDto.DeliverableRow> rows, 
                                       List<DeadlineCardDto> upcomingDeadlines, List<ReminderDto> reminders) {
        try {
            String groupTitle = safeGetGroupTitle(group);
            String prompt = safeBuildAiPrompt(groupTitle != null ? groupTitle : "Your Group", rows, upcomingDeadlines, reminders);
            String result = aiService.getCompletion(prompt).block();
            return result != null ? result : "AI InsightHub: Stay on track with your upcoming deadlines!";
        } catch (Exception e) {
            System.err.println("  Error getting AI insight: " + e.getMessage());
            return "AI InsightHub: Review your next deadline and address any revision requests early.";
        }
    }

    private String safeBuildAiPrompt(String groupTitle, List<StudentWorkspaceDto.DeliverableRow> rows,
                                       List<DeadlineCardDto> upcomingDeadlines, List<ReminderDto> reminders) {
        long pending = rows != null ? countByStatus(rows, SubmissionStatus.PENDING) : 0;
        long needsRevision = rows != null ? countByStatus(rows, SubmissionStatus.NEEDS_REVISION) : 0;
        long late = rows != null ? countByStatus(rows, SubmissionStatus.LATE) : 0;
        
        String nextDeadline = "No upcoming deadlines";
        if (upcomingDeadlines != null && !upcomingDeadlines.isEmpty() && upcomingDeadlines.get(0) != null) {
            DeadlineCardDto first = upcomingDeadlines.get(0);
            String name = first.deliverableName() != null ? first.deliverableName() : "Deliverable";
            String countdown = first.countdownLabel() != null ? first.countdownLabel() : "soon";
            nextDeadline = name + " in " + countdown;
        }
        
        String reminderSnippet = "No reminders";
        if (reminders != null && !reminders.isEmpty() && reminders.get(0) != null) {
            reminderSnippet = reminders.get(0).message() != null ? reminders.get(0).message() : reminderSnippet;
        }
        
        return String.format(
            "You are InsightHub AI for IntelliTrack. Write 3 short bullet points for student group '%s'. " +
            "Use the following facts: pending=%d, needsRevision=%d, late=%d, nextDeadline='%s', reminder='%s'. " +
            "Each bullet should be 10-16 words, actionable, academic, and professional. " +
            "Format exactly as:\n- ...\n- ...\n- ...",
            groupTitle, pending, needsRevision, late, nextDeadline, reminderSnippet);
    }

    // ==============================================================
    // EMPTY WORKSPACE BUILDERS (FOR STABLE FALLBACKS)
    // ==============================================================

    private StudentWorkspaceDto buildEmptyWorkspace(User student, String aiInsight) {
        String studentName = safeGetStudentName(student);
        return new StudentWorkspaceDto(
            new StudentWorkspaceDto.Header(
                student.getId(),
                studentName,
                null,
                null,
                null,
                null,
                null),
            new StudentWorkspaceDto.SubmissionOverview(0, 0, 0, 0, 0, 0, 0, 0),
            List.of(),
            List.of(),
            List.of(),
            aiInsight,
            List.of(),
            List.of());
    }

    private StudentWorkspaceDto buildEmptyWorkspaceForError(Long userId, String errorMessage) {
        return new StudentWorkspaceDto(
            new StudentWorkspaceDto.Header(
                userId,
                "Student",
                null,
                null,
                null,
                null,
                null),
            new StudentWorkspaceDto.SubmissionOverview(0, 0, 0, 0, 0, 0, 0, 0),
            List.of(),
            List.of(),
            List.of(),
            "Error loading workspace. Please contact support.",
            List.of(),
            List.of());
    }

    private StudentWorkspaceDto buildEmptyWorkspaceForError(Long userId, String studentName, String errorMessage) {
        return new StudentWorkspaceDto(
            new StudentWorkspaceDto.Header(
                userId,
                studentName,
                null,
                null,
                null,
                null,
                null),
            new StudentWorkspaceDto.SubmissionOverview(0, 0, 0, 0, 0, 0, 0, 0),
            List.of(),
            List.of(),
            List.of(),
            errorMessage,
            List.of(),
            List.of());
    }
}
