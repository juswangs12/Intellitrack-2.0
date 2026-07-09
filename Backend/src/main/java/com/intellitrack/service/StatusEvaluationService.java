package com.intellitrack.service;

import com.intellitrack.dto.DeliverableStatusDto;
import com.intellitrack.dto.GroupStatusSummaryDto;
import com.intellitrack.entity.Deliverable;
import com.intellitrack.entity.ProjectGroup;
import com.intellitrack.entity.Submission;
import com.intellitrack.entity.SubmissionStatus;
import com.intellitrack.repository.DeliverableRepository;
import com.intellitrack.repository.ProjectGroupRepository;
import com.intellitrack.repository.SubmissionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@Transactional(readOnly = true)
public class StatusEvaluationService {

    private final SubmissionRepository submissionRepository;
    private final DeliverableRepository deliverableRepository;
    private final ProjectGroupRepository projectGroupRepository;

    public StatusEvaluationService(
            SubmissionRepository submissionRepository,
            DeliverableRepository deliverableRepository,
            ProjectGroupRepository projectGroupRepository) {
        this.submissionRepository = submissionRepository;
        this.deliverableRepository = deliverableRepository;
        this.projectGroupRepository = projectGroupRepository;
    }

    public List<DeliverableStatusDto> getGroupStatuses(Long groupId) {
        List<Deliverable> deliverables = deliverableRepository.findAll();
        List<DeliverableStatusDto> statuses = new ArrayList<>();
        for (Deliverable deliverable : deliverables) {
            Submission submission = submissionRepository.findByGroupIdAndDeliverableId(groupId, deliverable.getId())
                    .orElse(null);
            statuses.add(toStatusDto(deliverable, submission));
        }
        return statuses;
    }

    public List<GroupStatusSummaryDto> getClassStatuses(Long adviserId) {
        List<ProjectGroup> groups = adviserId == null
                ? projectGroupRepository.findAll()
                : projectGroupRepository.findByAdviserId(adviserId);

        List<Deliverable> deliverables = deliverableRepository.findAll();
        long totalDeliverables = deliverables.size();

        return groups.stream().map(group -> {
            List<Submission> submissions = submissionRepository.findByGroupId(group.getId());
            long onTime = submissions.stream()
                    .filter(s -> computeStatus(s, s.getDeliverable().getDueAt()) == SubmissionStatus.SUBMITTED_ON_TIME)
                    .count();
            long late = submissions.stream()
                    .filter(s -> computeStatus(s, s.getDeliverable().getDueAt()) == SubmissionStatus.SUBMITTED_LATE)
                    .count();
            long submitted = onTime + late;
            long pending = totalDeliverables - submitted;

            return new GroupStatusSummaryDto(
                    group.getId(),
                    group.getCode(),
                    group.getTitle(),
                    submitted,
                    pending,
                    late,
                    totalDeliverables);
        }).toList();
    }

    public SubmissionStatus computeStatus(Submission submission, LocalDateTime dueAt) {
        if (submission == null) {
            if (dueAt == null) {
                return SubmissionStatus.PENDING;
            }

            if (LocalDateTime.now().isAfter(dueAt)) {
                return SubmissionStatus.OVERDUE;
            }

            return SubmissionStatus.NOT_SUBMITTED;
        }

        // Normalize legacy SUBMITTED → SUBMITTED_ON_TIME before any further checks
        SubmissionStatus existing = submission.getStatus();
        if (existing != null && existing.normalize() != existing) {
            return existing.normalize();
        }

        if (submission.getSubmittedAt() == null) {
            if (dueAt == null) {
                return SubmissionStatus.PENDING;
            }

            if (LocalDateTime.now().isAfter(dueAt)) {
                return SubmissionStatus.OVERDUE;
            }

            return SubmissionStatus.NOT_SUBMITTED;
        }

        if (dueAt == null) {
            return SubmissionStatus.SUBMITTED_ON_TIME;
        }

        if (submission.getSubmittedAt().isAfter(dueAt)) {
            return SubmissionStatus.SUBMITTED_LATE;
        }

        return SubmissionStatus.SUBMITTED_ON_TIME;
    }

    private DeliverableStatusDto toStatusDto(Deliverable deliverable, Submission submission) {
        LocalDateTime dueAt = deliverable.getDueAt();
        SubmissionStatus derivedStatus = computeStatus(submission, dueAt);
        long hoursRemaining = dueAt == null ? 0
                : Duration.between(LocalDateTime.now(), dueAt).toHours();

        return new DeliverableStatusDto(
                deliverable.getId(),
                deliverable.getName(),
                deliverable.getStage(),
                derivedStatus.name(),
                dueAt,
                submission == null ? null : submission.getSubmittedAt(),
                hoursRemaining,
                0);
    }
}
