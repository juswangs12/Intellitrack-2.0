package com.intellitrack.service;

import com.intellitrack.dto.DeadlineCardDto;
import com.intellitrack.dto.ReminderDto;
import com.intellitrack.dto.StudentWorkspaceDto;
import com.intellitrack.entity.AdviserFeedback;
import com.intellitrack.entity.Deadline;
import com.intellitrack.entity.Deliverable;
import com.intellitrack.entity.ProjectGroup;
import com.intellitrack.entity.Submission;
import com.intellitrack.entity.SubmissionStatus;
import com.intellitrack.entity.User;
import com.intellitrack.exception.ResourceNotFoundException;
import com.intellitrack.repository.AdviserFeedbackRepository;
import com.intellitrack.repository.DeadlineRepository;
import com.intellitrack.repository.DeliverableRepository;
import com.intellitrack.repository.ProjectGroupRepository;
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
import java.util.Objects;

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

    public StudentWorkspaceService(
            UserRepository userRepository,
            ProjectGroupRepository projectGroupRepository,
            DeliverableRepository deliverableRepository,
            DeadlineRepository deadlineRepository,
            SubmissionRepository submissionRepository,
            AdviserFeedbackRepository adviserFeedbackRepository,
            DeadlineMonitoringService deadlineMonitoringService,
            StatusEvaluationService statusEvaluationService,
            AiService aiService) {
        this.userRepository = userRepository;
        this.projectGroupRepository = projectGroupRepository;
        this.deliverableRepository = deliverableRepository;
        this.deadlineRepository = deadlineRepository;
        this.submissionRepository = submissionRepository;
        this.adviserFeedbackRepository = adviserFeedbackRepository;
        this.deadlineMonitoringService = deadlineMonitoringService;
        this.statusEvaluationService = statusEvaluationService;
        this.aiService = aiService;
    }

    public StudentWorkspaceDto build(Long studentUserId) {
        System.out.println("=== StudentWorkspaceService.build called for userId: " + studentUserId + " ===");
        try {
            User student = userRepository.findById(studentUserId)
                    .orElseThrow(() -> new ResourceNotFoundException("User not found"));

            if (!"student".equalsIgnoreCase(student.getRole())) {
                throw new IllegalArgumentException("User is not a student");
            }

            ProjectGroup group = student.getGroup();
            if (group == null) {
                System.out.println("Student has no group, returning empty workspace");
                return new StudentWorkspaceDto(
                        new StudentWorkspaceDto.Header(
                                student.getId(),
                                student.getFirstName() + " " + student.getLastName(),
                                null,
                                null,
                                null,
                                null,
                                null),
                        new StudentWorkspaceDto.SubmissionOverview(0, 0, 0, 0, 0, 0, 0, 0),
                        List.of(),
                        List.of(),
                        List.of(),
                        aiService.getCompletion("Create a short supportive onboarding note for a student with no assigned group in IntelliTrack.")
                                .block(),
                        List.of(),
                        List.of());
            }

            System.out.println("Student is in group: " + group.getId());

            ProjectGroup hydratedGroup = projectGroupRepository.findById(group.getId())
                    .orElseThrow(() -> new ResourceNotFoundException("Group not found"));

            List<Deliverable> deliverables = deliverableRepository.findByActiveTrue();
            System.out.println("Found " + deliverables.size() + " active deliverables");

            Map<Long, Deadline> deadlinesByDeliverableId = new HashMap<>();
            for (Deadline deadline : deadlineRepository.findAll()) {
                if (deadline.getDeliverable() != null && deadline.getDeliverable().getId() != null) {
                    deadlinesByDeliverableId.put(deadline.getDeliverable().getId(), deadline);
                }
            }

            Map<Long, Submission> submissionsByDeliverableId = new HashMap<>();
            for (Submission submission : submissionRepository.findByGroupId(hydratedGroup.getId())) {
                if (submission.getDeliverable() != null && submission.getDeliverable().getId() != null) {
                    submissionsByDeliverableId.put(submission.getDeliverable().getId(), submission);
                }
            }
            System.out.println("Found " + submissionsByDeliverableId.size() + " submissions for group");

            List<StudentWorkspaceDto.DeliverableRow> rows = deliverables.stream()
                    .map(deliverable -> {
                        Deadline deadline = deadlinesByDeliverableId.get(deliverable.getId());
                        Submission submission = submissionsByDeliverableId.get(deliverable.getId());
                        SubmissionStatus status = statusEvaluationService.computeStatus(submission, deadline);
                        Long hoursRemaining = deadline == null ? null : java.time.Duration.between(LocalDateTime.now(), deadline.getDueAt()).toHours();
                        return new StudentWorkspaceDto.DeliverableRow(
                                deliverable.getId(),
                                deliverable.getName(),
                                deliverable.getStage(),
                                status.name(),
                                deadline == null ? null : deadline.getDueAt(),
                                hoursRemaining,
                                submission == null ? 0 : submission.getRevisionCount(),
                                submission == null ? null : submission.getSubmittedAt(),
                                submission == null ? null : submission.getId(),
                                submission == null ? null : submission.getVersionNumber(),
                                submission != null && submission.getAiSummary() != null && !submission.getAiSummary().isBlank());
                    })
                    .sorted(Comparator
                            .comparing((StudentWorkspaceDto.DeliverableRow row) -> row.dueAt() == null)
                            .thenComparing(row -> row.dueAt() == null ? LocalDateTime.MAX : row.dueAt()))
                    .toList();
            System.out.println("Built " + rows.size() + " deliverable rows");

            long total = deliverables.size();
            long approved = rows.stream().filter(r -> SubmissionStatus.APPROVED.name().equals(r.status())).count();
            long rejected = rows.stream().filter(r -> SubmissionStatus.REJECTED.name().equals(r.status())).count();
            long needsRevision = rows.stream().filter(r -> SubmissionStatus.NEEDS_REVISION.name().equals(r.status())).count();
            long late = rows.stream().filter(r -> SubmissionStatus.LATE.name().equals(r.status())).count();
            long pending = rows.stream().filter(r -> SubmissionStatus.PENDING.name().equals(r.status())).count();
            long underReview = rows.stream().filter(r -> SubmissionStatus.SUBMITTED.name().equals(r.status()) || SubmissionStatus.UPDATED.name().equals(r.status())).count();
            long submitted = total - pending;

            List<DeadlineCardDto> deadlineCards = deadlineMonitoringService.getActiveDeadlines(hydratedGroup.getId()).stream()
                    .filter(card -> card.dueAt() != null)
                    .sorted(Comparator.comparing(DeadlineCardDto::hoursRemaining))
                    .toList();

            List<DeadlineCardDto> upcomingDeadlines = deadlineCards.stream()
                    .filter(card -> card.hoursRemaining() >= 0)
                    .limit(6)
                    .toList();

            List<ReminderDto> reminders = deadlineMonitoringService.getReminders(studentUserId).stream()
                    .limit(6)
                    .toList();

            List<StudentWorkspaceDto.FeedbackItem> recentFeedback = adviserFeedbackRepository
                    .findBySubmission_Group_IdOrderByEvaluatedAtDesc(hydratedGroup.getId()).stream()
                    .limit(6)
                    .map(feedback -> {
                        String evaluatorName = feedback.getAdviser() == null
                                ? "Adviser"
                                : (feedback.getAdviser().getFirstName() + " " + feedback.getAdviser().getLastName()).trim();
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
                                evaluatorName);
                    })
                    .toList();

            List<StudentWorkspaceDto.TimelineItem> timeline = buildTimeline(rows, recentFeedback).stream()
                    .sorted(Comparator.comparing(StudentWorkspaceDto.TimelineItem::occurredAt, Comparator.nullsLast(Comparator.naturalOrder())).reversed())
                    .limit(20)
                    .toList();

            String adviserName = hydratedGroup.getAdviser() == null
                    ? null
                    : (hydratedGroup.getAdviser().getFirstName() + " " + hydratedGroup.getAdviser().getLastName()).trim();

            String aiPrompt = buildAiPrompt(hydratedGroup.getTitle(), rows, upcomingDeadlines, reminders);
            String aiInsight;
            try {
                aiInsight = aiService.getCompletion(aiPrompt).block();
            } catch (Exception e) {
                aiInsight = "AI InsightHub: Review your next deadline and address any revision requests early.";
            }

            System.out.println("=== StudentWorkspaceService.build complete ===");
            return new StudentWorkspaceDto(
                    new StudentWorkspaceDto.Header(
                            student.getId(),
                            (student.getFirstName() + " " + student.getLastName()).trim(),
                            hydratedGroup.getId(),
                            hydratedGroup.getCode(),
                            hydratedGroup.getTitle(),
                            hydratedGroup.getAdviser() == null ? null : hydratedGroup.getAdviser().getId(),
                            adviserName),
                    new StudentWorkspaceDto.SubmissionOverview(
                            total,
                            submitted,
                            pending,
                            late,
                            underReview,
                            needsRevision,
                            approved,
                            rejected),
                    rows,
                    upcomingDeadlines,
                    reminders,
                    aiInsight,
                    recentFeedback,
                    timeline);
        } catch (Exception e) {
            System.err.println("=== ERROR in StudentWorkspaceService.build ===");
            e.printStackTrace();
            throw e;
        }
    }

    private List<StudentWorkspaceDto.TimelineItem> buildTimeline(
            List<StudentWorkspaceDto.DeliverableRow> rows,
            List<StudentWorkspaceDto.FeedbackItem> recentFeedback) {
        List<StudentWorkspaceDto.TimelineItem> items = new ArrayList<>();

        for (StudentWorkspaceDto.DeliverableRow row : rows) {
            if (row.submittedAt() != null) {
                String title = "Submission uploaded";
                String detail = row.deliverableName() + " • Version " + (row.versionNumber() == null ? 1 : row.versionNumber());
                items.add(new StudentWorkspaceDto.TimelineItem(
                        "SUBMISSION",
                        title,
                        detail,
                        row.submittedAt(),
                        row.deliverableId(),
                        row.submissionId()));
            }
            if (row.dueAt() != null) {
                String title = "Deadline scheduled";
                String detail = row.deliverableName() + " due";
                items.add(new StudentWorkspaceDto.TimelineItem(
                        "DEADLINE",
                        title,
                        detail,
                        row.dueAt(),
                        row.deliverableId(),
                        row.submissionId()));
            }
        }

        for (StudentWorkspaceDto.FeedbackItem feedback : recentFeedback) {
            if (feedback.evaluatedAt() == null) continue;
            String title = "Feedback posted";
            String detail = (feedback.deliverableName() == null ? "Deliverable" : feedback.deliverableName())
                    + " • " + (feedback.status() == null ? "UPDATED" : feedback.status());
            items.add(new StudentWorkspaceDto.TimelineItem(
                    "FEEDBACK",
                    title,
                    detail,
                    feedback.evaluatedAt(),
                    feedback.deliverableId(),
                    feedback.submissionId()));
        }

        return items;
    }

    private String buildAiPrompt(
            String groupTitle,
            List<StudentWorkspaceDto.DeliverableRow> rows,
            List<DeadlineCardDto> upcomingDeadlines,
            List<ReminderDto> reminders) {

        long pending = rows.stream().filter(r -> SubmissionStatus.PENDING.name().equals(r.status())).count();
        long needsRevision = rows.stream().filter(r -> SubmissionStatus.NEEDS_REVISION.name().equals(r.status())).count();
        long late = rows.stream().filter(r -> SubmissionStatus.LATE.name().equals(r.status())).count();

        String nextDeadline = upcomingDeadlines.isEmpty()
                ? "No upcoming deadlines found."
                : upcomingDeadlines.get(0).deliverableName() + " in " + upcomingDeadlines.get(0).countdownLabel();

        String reminderSnippet = reminders.isEmpty()
                ? "No reminders."
                : reminders.get(0).message();

        return String.format(
                "You are InsightHub AI for IntelliTrack. Write 3 short bullet points for student group '%s'. " +
                "Use the following facts: pending=%d, needsRevision=%d, late=%d, nextDeadline='%s', reminder='%s'. " +
                "Each bullet should be 10-16 words, actionable, academic, and professional. " +
                "Format exactly as:\n- ...\n- ...\n- ...",
                groupTitle, pending, needsRevision, late, nextDeadline, reminderSnippet);
    }
}

