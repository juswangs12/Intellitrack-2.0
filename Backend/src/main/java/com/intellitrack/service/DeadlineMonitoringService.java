package com.intellitrack.service;

import com.intellitrack.dto.CalendarDeadlineDto;
import com.intellitrack.dto.DeadlineCardDto;
import com.intellitrack.dto.ReminderDto;
import com.intellitrack.dto.RiskAssessmentDto;
import com.intellitrack.entity.Deadline;
import com.intellitrack.entity.Deliverable;
import com.intellitrack.entity.ProjectGroup;
import com.intellitrack.entity.RiskAssessmentLog;
import com.intellitrack.entity.Submission;
import com.intellitrack.entity.SubmissionStatus;
import com.intellitrack.entity.User;
import com.intellitrack.repository.DeadlineRepository;
import com.intellitrack.repository.DeliverableRepository;
import com.intellitrack.repository.ProjectGroupRepository;
import com.intellitrack.repository.ReminderLogRepository;
import com.intellitrack.repository.RiskAssessmentLogRepository;
import com.intellitrack.repository.SubmissionRepository;
import com.intellitrack.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@Transactional(readOnly = true)
public class DeadlineMonitoringService {

    private final UserRepository userRepository;
    private final ProjectGroupRepository projectGroupRepository;
    private final DeliverableRepository deliverableRepository;
    private final DeadlineRepository deadlineRepository;
    private final SubmissionRepository submissionRepository;
    private final ReminderLogRepository reminderLogRepository;
    private final RiskAssessmentLogRepository riskAssessmentLogRepository;
    private final StatusEvaluationService statusEvaluationService;
    private final AISubmissionRiskEngine aiSubmissionRiskEngine;
    private final SmartReminderService smartReminderService;

    public DeadlineMonitoringService(
            UserRepository userRepository,
            ProjectGroupRepository projectGroupRepository,
            DeliverableRepository deliverableRepository,
            DeadlineRepository deadlineRepository,
            SubmissionRepository submissionRepository,
            ReminderLogRepository reminderLogRepository,
            RiskAssessmentLogRepository riskAssessmentLogRepository,
            StatusEvaluationService statusEvaluationService,
            AISubmissionRiskEngine aiSubmissionRiskEngine,
            SmartReminderService smartReminderService) {
        this.userRepository = userRepository;
        this.projectGroupRepository = projectGroupRepository;
        this.deliverableRepository = deliverableRepository;
        this.deadlineRepository = deadlineRepository;
        this.submissionRepository = submissionRepository;
        this.reminderLogRepository = reminderLogRepository;
        this.riskAssessmentLogRepository = riskAssessmentLogRepository;
        this.statusEvaluationService = statusEvaluationService;
        this.aiSubmissionRiskEngine = aiSubmissionRiskEngine;
        this.smartReminderService = smartReminderService;
    }

    public List<DeadlineCardDto> getActiveDeadlines(Long groupId) {
        ProjectGroup group = projectGroupRepository.findById(groupId)
                .orElseThrow(() -> new IllegalArgumentException("Group not found: " + groupId));

        return deliverableRepository.findAll().stream()
                .map(deliverable -> toDeadlineCard(group, deliverable))
                .toList();
    }

    public List<CalendarDeadlineDto> getCalendar(int year, int month) {
        LocalDateTime start = LocalDate.of(year, month, 1).atStartOfDay();
        LocalDateTime end = start.plusMonths(1).minusSeconds(1);

        return deadlineRepository.findByDueAtBetween(start, end).stream()
                .map(deadline -> new CalendarDeadlineDto(
                        deadline.getId(),
                        deadline.getDeliverable().getId(),
                        deadline.getDeliverable().getName(),
                        deadline.getDeliverable().getStage(),
                        deadline.getDueAt()))
                .toList();
    }

    public List<ReminderDto> getReminders(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));

        if (user.getGroup() == null) {
            return List.of();
        }

        return deliverableRepository.findAll().stream()
                .map(deliverable -> buildReminderForDeliverable(user.getGroup(), deliverable))
                .filter(reminder -> reminder.hoursRemaining() <= 168 || "AT_RISK".equals(reminder.riskLevel()))
                .toList();
    }

    @Transactional
    public List<ReminderDto> buildDispatchQueue() {
        List<ReminderDto> queue = new ArrayList<>();

        for (ProjectGroup group : projectGroupRepository.findAll()) {
            for (Deliverable deliverable : deliverableRepository.findAll()) {
                Deadline deadline = deadlineRepository.findByDeliverableId(deliverable.getId()).orElse(null);
                Submission submission = submissionRepository
                        .findByGroupIdAndDeliverableId(group.getId(), deliverable.getId()).orElse(null);
                SubmissionStatus status = statusEvaluationService.computeStatus(submission, deadline);
                long hoursRemaining = deadline == null ? 0
                        : Duration.between(LocalDateTime.now(), deadline.getDueAt()).toHours();
                long previousLateCount = submissionRepository.findByGroupId(group.getId()).stream()
                        .filter(item -> statusEvaluationService.computeStatus(item,
                                deadlineRepository.findByDeliverableId(item.getDeliverable().getId())
                                        .orElse(null)) == SubmissionStatus.LATE)
                        .count();

                RiskAssessmentDto risk = aiSubmissionRiskEngine.assess(hoursRemaining, status,
                        submission == null ? 0 : submission.getRevisionCount(), previousLateCount);
                if (hoursRemaining <= 72 || "AT_RISK".equals(risk.riskLevel())) {
                    ReminderDto reminder = smartReminderService.buildReminder(group.getId(), deliverable.getId(),
                            deliverable.getName(), risk, hoursRemaining);
                    queue.add(reminder);

                    RiskAssessmentLog riskLog = new RiskAssessmentLog();
                    riskLog.setGroup(group);
                    riskLog.setDeliverable(deliverable);
                    riskLog.setRiskScore(risk.riskScore());
                    riskLog.setRiskLevel(risk.riskLevel());
                    riskAssessmentLogRepository.save(riskLog);
                }
            }
        }

        return queue;
    }

    private DeadlineCardDto toDeadlineCard(ProjectGroup group, Deliverable deliverable) {
        Deadline deadline = deadlineRepository.findByDeliverableId(deliverable.getId()).orElse(null);
        Submission submission = submissionRepository.findByGroupIdAndDeliverableId(group.getId(), deliverable.getId())
                .orElse(null);
        SubmissionStatus status = statusEvaluationService.computeStatus(submission, deadline);
        long hoursRemaining = deadline == null ? 0
                : Duration.between(LocalDateTime.now(), deadline.getDueAt()).toHours();
        long previousLateCount = submissionRepository.findByGroupId(group.getId()).stream()
                .filter(item -> statusEvaluationService.computeStatus(item,
                        deadlineRepository.findByDeliverableId(item.getDeliverable().getId())
                                .orElse(null)) == SubmissionStatus.LATE)
                .count();

        RiskAssessmentDto risk = aiSubmissionRiskEngine.assess(hoursRemaining, status,
                submission == null ? 0 : submission.getRevisionCount(), previousLateCount);

        return new DeadlineCardDto(
                deadline == null ? null : deadline.getId(),
                deliverable.getId(),
                deliverable.getName(),
                deliverable.getStage(),
                deadline == null ? null : deadline.getDueAt(),
                status.name(),
                hoursRemaining,
                submission == null ? 0 : submission.getRevisionCount(),
                risk.riskScore(),
                risk.riskLevel(),
                risk.explanation(),
                formatCountdown(hoursRemaining));
    }

    private ReminderDto buildReminderForDeliverable(ProjectGroup group, Deliverable deliverable) {
        Deadline deadline = deadlineRepository.findByDeliverableId(deliverable.getId()).orElse(null);
        Submission submission = submissionRepository.findByGroupIdAndDeliverableId(group.getId(), deliverable.getId())
                .orElse(null);
        SubmissionStatus status = statusEvaluationService.computeStatus(submission, deadline);
        long hoursRemaining = deadline == null ? 0
                : Duration.between(LocalDateTime.now(), deadline.getDueAt()).toHours();
        long previousLateCount = submissionRepository.findByGroupId(group.getId()).stream()
                .filter(item -> statusEvaluationService.computeStatus(item,
                        deadlineRepository.findByDeliverableId(item.getDeliverable().getId())
                                .orElse(null)) == SubmissionStatus.LATE)
                .count();

        RiskAssessmentDto risk = aiSubmissionRiskEngine.assess(hoursRemaining, status,
                submission == null ? 0 : submission.getRevisionCount(), previousLateCount);
        return smartReminderService.buildReminder(group.getId(), deliverable.getId(), deliverable.getName(), risk,
                hoursRemaining);
    }

    private String formatCountdown(long hoursRemaining) {
        if (hoursRemaining < 0) {
            return Math.abs(hoursRemaining) + "h overdue";
        }
        if (hoursRemaining < 24) {
            return hoursRemaining + "h remaining";
        }
        return (hoursRemaining / 24) + "d remaining";
    }
}