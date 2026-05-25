package com.intellitrack.service;

import com.intellitrack.dto.DeadlineCardDto;
import com.intellitrack.dto.ReminderDto;
import com.intellitrack.dto.StudentWorkspaceDto;
import com.intellitrack.entity.AdviserFeedback;
import com.intellitrack.entity.Deliverable;
import com.intellitrack.entity.ProjectGroup;
import com.intellitrack.entity.StudentEnrollment;
import com.intellitrack.entity.Submission;
import com.intellitrack.entity.SubmissionStatus;
import com.intellitrack.entity.User;
import com.intellitrack.repository.AdviserFeedbackRepository;
import com.intellitrack.repository.DeliverableRepository;
import com.intellitrack.repository.ProjectGroupRepository;
import com.intellitrack.repository.StudentEnrollmentRepository;
import com.intellitrack.repository.SubmissionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@SuppressWarnings({ "null", "unused" })
@Service
@Transactional(readOnly = true)
public class StudentWorkspaceService {

    private final ProjectGroupRepository projectGroupRepository;
    private final DeliverableRepository deliverableRepository;
    private final SubmissionRepository submissionRepository;
    private final AdviserFeedbackRepository adviserFeedbackRepository;
    private final DeadlineMonitoringService deadlineMonitoringService;
    private final StatusEvaluationService statusEvaluationService;
    private final StudentEnrollmentRepository studentEnrollmentRepository;
    private final AiService aiService;

    public StudentWorkspaceService(
            ProjectGroupRepository projectGroupRepository,
            DeliverableRepository deliverableRepository,
            SubmissionRepository submissionRepository,
            AdviserFeedbackRepository adviserFeedbackRepository,
            DeadlineMonitoringService deadlineMonitoringService,
            StatusEvaluationService statusEvaluationService,
            StudentEnrollmentRepository studentEnrollmentRepository,
            AiService aiService) {
        this.projectGroupRepository = projectGroupRepository;
        this.deliverableRepository = deliverableRepository;
        this.submissionRepository = submissionRepository;
        this.adviserFeedbackRepository = adviserFeedbackRepository;
        this.deadlineMonitoringService = deadlineMonitoringService;
        this.statusEvaluationService = statusEvaluationService;
        this.studentEnrollmentRepository = studentEnrollmentRepository;
        this.aiService = aiService;
    }

    public StudentWorkspaceDto build(User student) {
        System.out.println("=== StudentWorkspaceService.build called for user: " + student.getEmail() + " ===");
        Long studentUserId = student.getId();

        try {
            System.out.println("  Student found: email=" + student.getEmail() + ", role=" + student.getRole());

            if (!"student".equalsIgnoreCase(student.getRole())) {
                System.err.println("  ERROR: User is not a student, role=" + student.getRole());
                return buildEmptyWorkspaceForError(studentUserId,
                        (student.getFirstName() != null ? student.getFirstName() : "User") + " " +
                                (student.getLastName() != null ? student.getLastName() : ""),
                        "Not a student");
            }

            List<StudentEnrollment> enrollments = safeFindEnrollments(studentUserId);
            System.out.println("  Found " + enrollments.size() + " enrollments");

            ProjectGroup group = safeGetGroupFromEnrollments(enrollments);

            if (group == null && student.getGroup() != null) {
                System.out.println("  No group from enrollments, using user.group fallback");
                try {
                    group = student.getGroup();
                    if (group != null && group.getId() != null) {
                        group.getTitle();
                    }
                } catch (Exception e) {
                    System.err.println("  Error loading user.group fallback: " + e.getMessage());
                    group = null;
                }
            }

            System.out.println("  Group determined: " + (group != null
                    ? "id=" + group.getId() + ", title=" + (group.getTitle() != null ? group.getTitle() : "null")
                    : "null"));

            if (group == null) {
                System.out.println("  No group found, returning empty workspace");
                return buildEmptyWorkspace(student, "No group assigned yet");
            }

            ProjectGroup hydratedGroup = projectGroupRepository.findByIdWithSubject(group.getId()).orElse(null);
            if (hydratedGroup == null) {
                hydratedGroup = group;
            }
            System.out.println(
                    "  Hydrated group: " + (hydratedGroup.getTitle() != null ? hydratedGroup.getTitle() : "null"));

            List<Deliverable> deliverables = safeGetDeliverables(hydratedGroup);
            Map<Long, Submission> submissionsByDeliverableId = safeGetSubmissionsMap(hydratedGroup);
            System.out.println("  Deliverables=" + deliverables.size() +
                    ", Submissions=" + submissionsByDeliverableId.size());

            List<StudentWorkspaceDto.DeliverableRow> rows = safeBuildDeliverableRows(
                    deliverables, submissionsByDeliverableId);
            System.out.println("  Built " + rows.size() + " deliverable rows");

            StudentWorkspaceDto.SubmissionOverview overview = safeCalculateOverview(rows, deliverables.size());

            List<DeadlineCardDto> deadlineCards = safeGetDeadlineCards(hydratedGroup);
            List<DeadlineCardDto> upcomingDeadlines = safeGetUpcomingDeadlines(deadlineCards);
            List<ReminderDto> reminders = safeGetReminders(studentUserId);
            List<StudentWorkspaceDto.FeedbackItem> recentFeedback = safeGetRecentFeedback(hydratedGroup);
            List<StudentWorkspaceDto.TimelineItem> timeline = safeBuildTimeline(rows, recentFeedback);

            String adviserName = safeGetAdviserName(hydratedGroup);
            String aiInsight = safeGetAiInsight(hydratedGroup, rows, upcomingDeadlines, reminders);

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

            String studentName = "Student";
            if (student != null) {
                try {
                    String firstName = student.getFirstName() != null ? student.getFirstName() : "";
                    String lastName = student.getLastName() != null ? student.getLastName() : "";
                    String fullName = (firstName + " " + lastName).trim();
                    if (!fullName.isEmpty()) {
                        studentName = fullName;
                    }
                } catch (Exception ex) {
                }
            }

            return buildEmptyWorkspaceForError(studentUserId, studentName, "System error loading workspace");
        }
    }

    private List<StudentEnrollment> safeFindEnrollments(Long userId) {
        try {
            List<StudentEnrollment> result = studentEnrollmentRepository.findByStudent_IdWithAllRelations(userId);
            return result != null ? result : List.of();
        } catch (Exception e) {
            System.err.println("  Error loading enrollments (falling back to basic): " + e.getMessage());
            e.printStackTrace();
            try {
                List<StudentEnrollment> basicResult = studentEnrollmentRepository.findByStudent_Id(userId);
                return basicResult != null ? basicResult : List.of();
            } catch (Exception ex2) {
                System.err.println("  Also failed loading basic enrollments: " + ex2.getMessage());
                return List.of();
            }
        }
    }

    private ProjectGroup safeGetGroupFromEnrollments(List<StudentEnrollment> enrollments) {
        if (enrollments == null || enrollments.isEmpty())
            return null;

        for (StudentEnrollment enrollment : enrollments) {
            if (enrollment == null)
                continue;

            try {
                List<ProjectGroup> groups = enrollment.getGroups();
                if (groups != null && !groups.isEmpty() && groups.get(0) != null) {
                    return groups.get(0);
                }
            } catch (Exception e) {
                System.err.println("  Error getting groups from enrollment: " + e.getMessage());
            }
        }

        return null;
    }

    private List<Deliverable> safeGetDeliverables(ProjectGroup group) {
        try {
            System.out.println("=== safeGetDeliverables called ===");
            System.out.println("GROUP ID: " + (group != null ? group.getId() : "null"));
            System.out.println("GROUP SUBJECT: " + (group != null && group.getSubject() != null ? group.getSubject().getName() + " (id=" + group.getSubject().getId() + ")" : "null"));
            
            if (group == null || group.getSubject() == null) {
                System.out.println("FALLING BACK TO ALL ACTIVE DELIVERABLES!");
                return deliverableRepository.findByActiveTrue();
            }

            Long subjectId = group.getSubject().getId();
            System.out.println("FILTERING BY SUBJECT ID: " + subjectId);
            List<Deliverable> deliverables = deliverableRepository.findByActiveTrueAndSubjectId(subjectId);
            System.out.println("DELIVERABLE COUNT: " + deliverables.size());
            deliverables.forEach(d -> System.out.println("  - " + d.getName() + " (subject=" + (d.getSubject() != null ? d.getSubject().getName() : "null") + ")"));
            return deliverables;
        } catch (Exception e) {
            System.err.println("  Error loading deliverables: " + e.getMessage());
            e.printStackTrace();
            return List.of();
        }
    }

    private Map<Long, Submission> safeGetSubmissionsMap(ProjectGroup group) {
        Map<Long, Submission> map = new HashMap<>();
        if (group == null || group.getId() == null)
            return map;

        try {
            List<Submission> submissions = submissionRepository.findByGroupId(group.getId());
            for (Submission submission : submissions) {
                if (submission != null && submission.getDeliverable() != null
                        && submission.getDeliverable().getId() != null) {
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

        if (deliverables == null)
            return rows;

        for (Deliverable deliverable : deliverables) {
            if (deliverable == null)
                continue;

            try {
                Submission submission = submissionsByDeliverableId.getOrDefault(deliverable.getId(), null);
                SubmissionStatus status = safeComputeStatus(submission, deliverable);

                Long hoursRemaining = null;
                if (deliverable.getDueAt() != null) {
                    try {
                        hoursRemaining = java.time.Duration.between(LocalDateTime.now(), deliverable.getDueAt())
                                .toHours();
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
                        submission == null ? null : submission.getSubmittedAt(),
                        submission == null ? null : submission.getId(),
                        submission == null ? null : submission.getVersionNumber(),
                        submission != null && submission.getAiSummary() != null
                                && !submission.getAiSummary().isBlank()));
            } catch (Exception e) {
                System.err
                        .println("  Error building row for deliverable " + deliverable.getId() + ": " + e.getMessage());
            }
        }

        rows.sort(Comparator
                .comparing((StudentWorkspaceDto.DeliverableRow row) -> row.dueAt() == null)
                .thenComparing(row -> row.dueAt() == null ? LocalDateTime.MAX : row.dueAt()));

        return rows;
    }

    private SubmissionStatus safeComputeStatus(Submission submission, Deliverable deliverable) {
        try {
            LocalDateTime dueAt = deliverable != null ? deliverable.getDueAt() : null;
            return statusEvaluationService.computeStatus(submission, dueAt);
        } catch (Exception e) {
            System.err.println("  Error computing status: " + e.getMessage());
            return SubmissionStatus.PENDING;
        }
    }

    private StudentWorkspaceDto.SubmissionOverview safeCalculateOverview(
            List<StudentWorkspaceDto.DeliverableRow> rows, long totalDeliverables) {

        if (rows == null) {
            return new StudentWorkspaceDto.SubmissionOverview(
                    totalDeliverables, 0, totalDeliverables, 0, 0, 0);
        }

        long onTime = countByStatus(rows, SubmissionStatus.SUBMITTED_ON_TIME);
        long late = countByStatus(rows, SubmissionStatus.SUBMITTED_LATE);
        long notSubmitted = countByStatus(rows, SubmissionStatus.NOT_SUBMITTED);
        long pending = countByStatus(rows, SubmissionStatus.PENDING);
        long submitted = onTime + late;

        return new StudentWorkspaceDto.SubmissionOverview(
                totalDeliverables, submitted, pending, late, onTime, notSubmitted);
    }

    private long countByStatus(List<StudentWorkspaceDto.DeliverableRow> rows, SubmissionStatus status) {
        if (rows == null)
            return 0;
        return rows.stream()
                .filter(r -> r != null && status.name().equals(r.status()))
                .count();
    }

    private List<DeadlineCardDto> safeGetDeadlineCards(ProjectGroup group) {
        if (group == null || group.getId() == null)
            return List.of();

        try {
            return deadlineMonitoringService.getActiveDeadlines(group.getId());
        } catch (Exception e) {
            System.err.println("  Error loading deadline cards: " + e.getMessage());
            return List.of();
        }
    }

    private List<DeadlineCardDto> safeGetUpcomingDeadlines(List<DeadlineCardDto> cards) {
        if (cards == null)
            return List.of();

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
            if (reminders == null)
                return List.of();
            return reminders.stream().limit(6).toList();
        } catch (Exception e) {
            System.err.println("  Error loading reminders: " + e.getMessage());
            return List.of();
        }
    }

    private List<StudentWorkspaceDto.FeedbackItem> safeGetRecentFeedback(ProjectGroup group) {
        if (group == null || group.getId() == null)
            return List.of();

        try {
            List<AdviserFeedback> feedbacks = adviserFeedbackRepository
                    .findBySubmission_Group_IdOrderByEvaluatedAtDesc(group.getId());

            if (feedbacks == null)
                return List.of();

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
        if (feedback == null)
            return null;

        try {
            Submission submission = feedback.getSubmission();
            Deliverable deliverable = submission == null ? null : submission.getDeliverable();

            return new StudentWorkspaceDto.FeedbackItem(
                    submission == null ? null : submission.getId(),
                    deliverable == null ? null : deliverable.getId(),
                    deliverable == null ? null : deliverable.getName(),
                    submission == null || submission.getStatus() == null ? null : submission.getStatus().name(),
                    feedback.getGeneralComments(),
                    feedback.getEvaluatedAt(),
                    safeGetEvaluatorName(feedback));
        } catch (Exception e) {
            System.err.println("  Error mapping feedback: " + e.getMessage());
            return null;
        }
    }

    private String safeGetEvaluatorName(AdviserFeedback feedback) {
        if (feedback == null || feedback.getAdviser() == null)
            return "Adviser";
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
                if (row == null)
                    continue;

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
                if (feedback == null || feedback.evaluatedAt() == null)
                    continue;

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
        String status = feedback.status() != null ? feedback.status() : "PENDING";
        return name + " • " + status;
    }

    private String safeGetStudentName(User student) {
        if (student == null)
            return "Student";
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
        if (g == null || g.getAdviser() == null)
            return null;
        User adviser = g.getAdviser();
        String firstName = adviser.getFirstName() != null ? adviser.getFirstName() : "";
        String lastName = adviser.getLastName() != null ? adviser.getLastName() : "";
        String fullName = (firstName + " " + lastName).trim();
        return fullName.isEmpty() ? null : fullName;
    }

    private String safeGetAiInsight(ProjectGroup group, List<StudentWorkspaceDto.DeliverableRow> rows,
            List<DeadlineCardDto> upcomingDeadlines, List<ReminderDto> reminders) {
        try {
            String groupTitle = group != null && group.getTitle() != null ? group.getTitle() : "Unknown Group";
            String prompt = safeBuildAiPrompt(groupTitle, rows, upcomingDeadlines, reminders);
            String systemPrompt = "You are InsightHub AI for IntelliTrack. " +
                    "Respond with plain text bullet points only. " +
                    "No JSON, no markdown headers, no code blocks.";
            String result = aiService.getCompletion(prompt, systemPrompt).block();
            return (result != null && !result.isBlank()) ? result : null;
        } catch (Exception e) {
            return null;
        }
    }

    private String safeBuildAiPrompt(String groupTitle, List<StudentWorkspaceDto.DeliverableRow> rows,
            List<DeadlineCardDto> upcomingDeadlines, List<ReminderDto> reminders) {
        long pending = rows != null ? countByStatus(rows, SubmissionStatus.PENDING) : 0;
        long late = rows != null ? countByStatus(rows, SubmissionStatus.SUBMITTED_LATE) : 0;
        long notSubmitted = rows != null ? countByStatus(rows, SubmissionStatus.NOT_SUBMITTED) : 0;

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
                        "Use the following facts: pending=%d, late=%d, notSubmitted=%d, nextDeadline='%s', reminder='%s'. "
                        +
                        "Each bullet should be 10-16 words, actionable, academic, and professional. " +
                        "Format exactly as:\n- ...\n- ...\n- ...",
                groupTitle, pending, late, notSubmitted, nextDeadline, reminderSnippet);
    }

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
                new StudentWorkspaceDto.SubmissionOverview(0, 0, 0, 0, 0, 0),
                List.of(),
                List.of(),
                List.of(),
                aiInsight,
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
                new StudentWorkspaceDto.SubmissionOverview(0, 0, 0, 0, 0, 0),
                List.of(),
                List.of(),
                List.of(),
                errorMessage,
                List.of(),
                List.of());
    }
}
