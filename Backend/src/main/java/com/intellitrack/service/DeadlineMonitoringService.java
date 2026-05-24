package com.intellitrack.service;

import com.intellitrack.dto.CalendarDeadlineDto;
import com.intellitrack.dto.DeadlineAdminDto;
import com.intellitrack.dto.DeadlineCardDto;
import com.intellitrack.dto.ReminderDto;
import com.intellitrack.dto.RiskAssessmentDto;
import com.intellitrack.entity.Deadline;
import com.intellitrack.entity.Deliverable;
import com.intellitrack.entity.ProjectGroup;
import com.intellitrack.entity.RiskAssessmentLog;
import com.intellitrack.entity.StudentEnrollment;
import com.intellitrack.entity.Submission;
import com.intellitrack.entity.SubmissionStatus;
import com.intellitrack.exception.ResourceNotFoundException;
import com.intellitrack.repository.DeadlineRepository;
import com.intellitrack.repository.DeliverableRepository;
import com.intellitrack.repository.ProjectGroupRepository;
import com.intellitrack.repository.RiskAssessmentLogRepository;
import com.intellitrack.repository.StudentEnrollmentRepository;
import com.intellitrack.repository.SubmissionRepository;
import com.intellitrack.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@Transactional(readOnly = true)
public class DeadlineMonitoringService {

    private final UserRepository userRepository;
    private final ProjectGroupRepository projectGroupRepository;
    private final DeliverableRepository deliverableRepository;
    private final DeadlineRepository deadlineRepository;
    private final SubmissionRepository submissionRepository;
    private final RiskAssessmentLogRepository riskAssessmentLogRepository;
    private final StatusEvaluationService statusEvaluationService;
    private final RiskAssessmentService riskAssessmentService;
    private final SmartReminderService smartReminderService;
    private final StudentEnrollmentRepository studentEnrollmentRepository;

    public DeadlineMonitoringService(
            UserRepository userRepository,
            ProjectGroupRepository projectGroupRepository,
            DeliverableRepository deliverableRepository,
            DeadlineRepository deadlineRepository,
            SubmissionRepository submissionRepository,
            RiskAssessmentLogRepository riskAssessmentLogRepository,
            StatusEvaluationService statusEvaluationService,
            RiskAssessmentService riskAssessmentService,
            SmartReminderService smartReminderService,
            StudentEnrollmentRepository studentEnrollmentRepository) {
        this.userRepository = userRepository;
        this.projectGroupRepository = projectGroupRepository;
        this.deliverableRepository = deliverableRepository;
        this.deadlineRepository = deadlineRepository;
        this.submissionRepository = submissionRepository;
        this.riskAssessmentLogRepository = riskAssessmentLogRepository;
        this.statusEvaluationService = statusEvaluationService;
        this.riskAssessmentService = riskAssessmentService;
        this.smartReminderService = smartReminderService;
        this.studentEnrollmentRepository = studentEnrollmentRepository;
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
        userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));

        final ProjectGroup group;
        List<StudentEnrollment> enrollments = studentEnrollmentRepository.findByStudent_Id(userId);
        // Sort enrollments by createdAt descending to get the most recent one first
        List<StudentEnrollment> sortedEnrollments = enrollments.stream()
                .sorted((e1, e2) -> {
                    if (e1.getCreatedAt() == null && e2.getCreatedAt() == null) return 0;
                    if (e1.getCreatedAt() == null) return 1;
                    if (e2.getCreatedAt() == null) return -1;
                    return e2.getCreatedAt().compareTo(e1.getCreatedAt());
                })
                .toList();
                
        if (!sortedEnrollments.isEmpty() && !sortedEnrollments.get(0).getGroups().isEmpty()) {
            group = sortedEnrollments.get(0).getGroups().get(0);
        } else {
            group = null;
        }

        if (group == null) {
            return List.of();
        }

        return deliverableRepository.findAll().stream()
                .map(deliverable -> buildReminderForDeliverable(group, deliverable))
                .filter(reminder -> reminder.hoursRemaining() <= 168 || "AT_RISK".equals(reminder.riskLevel()))
                .toList();
    }

    public List<DeadlineAdminDto> getAdminDeadlines() {
        Map<Long, Deadline> byDeliverableId = new HashMap<>();
        for (Deadline deadline : deadlineRepository.findAll()) {
            if (deadline.getDeliverable() != null && deadline.getDeliverable().getId() != null) {
                byDeliverableId.put(deadline.getDeliverable().getId(), deadline);
            }
        }

        return deliverableRepository.findAll().stream()
                .map(deliverable -> {
                    Deadline deadline = byDeliverableId.get(deliverable.getId());

                    LocalDateTime dueAt = deliverable.getDueAt();
                    String academicTerm = deliverable.getAcademicTerm();

                    if (dueAt == null && deadline != null) {
                        dueAt = deadline.getDueAt();
                    }
                    if (academicTerm == null && deadline != null) {
                        academicTerm = deadline.getAcademicTerm();
                    }

                    return new DeadlineAdminDto(
                            deliverable.getId(),
                            deliverable.getName(),
                            deliverable.getStage(),
                            deliverable.isActive(),
                            deadline == null ? null : deadline.getId(),
                            dueAt,
                            academicTerm);
                })
                .toList();
    }

    @Transactional
    public Deadline createDeadline(Long deliverableId, LocalDateTime dueAt, String academicTerm) {
        Deliverable deliverable = deliverableRepository.findById(deliverableId)
                .orElseThrow(() -> new ResourceNotFoundException("Deliverable not found"));

        deliverable.setDueAt(dueAt);
        deliverable.setAcademicTerm(academicTerm);
        deliverable = deliverableRepository.save(deliverable);

        Deadline deadline = deadlineRepository.findByDeliverableId(deliverableId)
                .orElse(new Deadline());

        deadline.setDeliverable(deliverable);
        deadline.setDueAt(dueAt);
        deadline.setAcademicTerm(academicTerm);

        return deadlineRepository.save(deadline);
    }

    @Transactional
    public Deadline updateDeadline(Long deadlineId, LocalDateTime dueAt, String academicTerm) {
        Deadline deadline = deadlineRepository.findById(deadlineId)
                .orElseThrow(() -> new ResourceNotFoundException("Deadline not found"));

        deadline.setDueAt(dueAt);
        deadline.setAcademicTerm(academicTerm);

        if (deadline.getDeliverable() != null) {
            Deliverable deliverable = deadline.getDeliverable();
            deliverable.setDueAt(dueAt);
            deliverable.setAcademicTerm(academicTerm);
            deliverableRepository.save(deliverable);
        }

        return deadlineRepository.save(deadline);
    }

    @Transactional
    public void deleteDeadline(Long deadlineId) {
        if (!deadlineRepository.existsById(deadlineId)) {
            throw new ResourceNotFoundException("Deadline not found");
        }
        deadlineRepository.deleteById(deadlineId);
    }

    @Transactional
    public Deliverable createDeliverable(String name, String stage) {
        Deliverable deliverable = new Deliverable();
        deliverable.setName(name);
        deliverable.setStage(stage);
        deliverable.setActive(true);
        return deliverableRepository.save(deliverable);
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
                                        .orElse(null)) == SubmissionStatus.SUBMITTED_LATE)
                        .count();

                RiskAssessmentDto risk = riskAssessmentService.assess(hoursRemaining, status,
                        previousLateCount);
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

        LocalDateTime dueAt = deliverable.getDueAt();
        if (dueAt == null && deadline != null) {
            dueAt = deadline.getDueAt();
        }

        SubmissionStatus status = statusEvaluationService.computeStatus(submission,
                dueAt != null ? new Deadline(deliverable) : deadline);
        long hoursRemaining = dueAt == null ? 0
                : Duration.between(LocalDateTime.now(), dueAt).toHours();
        long previousLateCount = submissionRepository.findByGroupId(group.getId()).stream()
                .filter(item -> statusEvaluationService.computeStatus(item,
                        deadlineRepository.findByDeliverableId(item.getDeliverable().getId())
                                .orElse(null)) == SubmissionStatus.SUBMITTED_LATE)
                .count();

        RiskAssessmentDto risk = riskAssessmentService.assess(hoursRemaining, status,
                previousLateCount);

        return new DeadlineCardDto(
                deadline == null ? null : deadline.getId(),
                deliverable.getId(),
                deliverable.getName(),
                deliverable.getStage(),
                deadline == null ? null : deadline.getDueAt(),
                status.name(),
                hoursRemaining,
                0,
                risk.riskScore(),
                risk.riskLevel(),
                risk.explanation(),
                formatCountdown(hoursRemaining));
    }

    private ReminderDto buildReminderForDeliverable(ProjectGroup group, Deliverable deliverable) {
        Deadline deadline = deadlineRepository.findByDeliverableId(deliverable.getId()).orElse(null);
        Submission submission = submissionRepository.findByGroupIdAndDeliverableId(group.getId(), deliverable.getId())
                .orElse(null);

        LocalDateTime dueAt = deliverable.getDueAt();
        if (dueAt == null && deadline != null) {
            dueAt = deadline.getDueAt();
        }

        SubmissionStatus status = statusEvaluationService.computeStatus(submission,
                dueAt != null ? new Deadline(deliverable) : deadline);
        long hoursRemaining = dueAt == null ? 0
                : Duration.between(LocalDateTime.now(), dueAt).toHours();
        long previousLateCount = submissionRepository.findByGroupId(group.getId()).stream()
                .filter(item -> statusEvaluationService.computeStatus(item,
                        deadlineRepository.findByDeliverableId(item.getDeliverable().getId())
                                .orElse(null)) == SubmissionStatus.SUBMITTED_LATE)
                .count();

        RiskAssessmentDto risk = riskAssessmentService.assess(hoursRemaining, status,
                previousLateCount);
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
