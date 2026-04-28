package com.intellitrack.service;

import com.intellitrack.dto.DeliverableStatusDto;
import com.intellitrack.dto.GroupStatusSummaryDto;
import com.intellitrack.entity.Deadline;
import com.intellitrack.entity.Deliverable;
import com.intellitrack.entity.ProjectGroup;
import com.intellitrack.entity.Submission;
import com.intellitrack.entity.SubmissionStatus;
import com.intellitrack.repository.DeadlineRepository;
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
    private final DeadlineRepository deadlineRepository;
    private final DeliverableRepository deliverableRepository;
    private final ProjectGroupRepository projectGroupRepository;

    public StatusEvaluationService(
            SubmissionRepository submissionRepository,
            DeadlineRepository deadlineRepository,
            DeliverableRepository deliverableRepository,
            ProjectGroupRepository projectGroupRepository) {
        this.submissionRepository = submissionRepository;
        this.deadlineRepository = deadlineRepository;
        this.deliverableRepository = deliverableRepository;
        this.projectGroupRepository = projectGroupRepository;
    }

    public List<DeliverableStatusDto> getGroupStatuses(Long groupId) {
        List<DeliverableStatusDto> statuses = new ArrayList<>();
        for (Deliverable deliverable : deliverableRepository.findAll()) {
            Deadline deadline = deadlineRepository.findByDeliverableId(deliverable.getId()).orElse(null);
            Submission submission = submissionRepository.findByGroupIdAndDeliverableId(groupId, deliverable.getId())
                    .orElse(null);
            statuses.add(toStatusDto(deliverable, deadline, submission));
        }
        return statuses;
    }

    public List<GroupStatusSummaryDto> getClassStatuses(Long adviserId) {
        List<ProjectGroup> groups = adviserId == null
                ? projectGroupRepository.findAll()
                : projectGroupRepository.findByAdviserId(adviserId);

        long totalDeliverables = deliverableRepository.count();
        return groups.stream().map(group -> {
            List<Submission> submissions = submissionRepository.findByGroupId(group.getId());
            long submitted = submissions.stream()
                    .filter(submission -> computeStatus(submission,
                            deadlineRepository.findByDeliverableId(submission.getDeliverable().getId())
                                    .orElse(null)) == SubmissionStatus.SUBMITTED)
                    .count();
            long late = submissions.stream().filter(submission -> computeStatus(submission, deadlineRepository
                    .findByDeliverableId(submission.getDeliverable().getId()).orElse(null)) == SubmissionStatus.LATE)
                    .count();
            long pending = totalDeliverables - submitted - late;

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

    public SubmissionStatus computeStatus(Submission submission, Deadline deadline) {
        if (deadline == null) {
            return submission == null ? SubmissionStatus.PENDING : submission.getStatus();
        }

        if (submission == null || submission.getSubmittedAt() == null) {
            return deadline.getDueAt().isBefore(LocalDateTime.now()) ? SubmissionStatus.LATE : SubmissionStatus.PENDING;
        }

        if (submission.getSubmittedAt().isAfter(deadline.getDueAt())) {
            return SubmissionStatus.LATE;
        }

        return submission.getRevisionCount() > 0 ? SubmissionStatus.UPDATED : SubmissionStatus.SUBMITTED;
    }

    private DeliverableStatusDto toStatusDto(Deliverable deliverable, Deadline deadline, Submission submission) {
        SubmissionStatus derivedStatus = computeStatus(submission, deadline);
        long hoursRemaining = deadline == null ? 0
                : Duration.between(LocalDateTime.now(), deadline.getDueAt()).toHours();

        return new DeliverableStatusDto(
                deliverable.getId(),
                deliverable.getName(),
                deliverable.getStage(),
                derivedStatus.name(),
                deadline == null ? null : deadline.getDueAt(),
                submission == null ? null : submission.getSubmittedAt(),
                hoursRemaining,
                submission == null ? 0 : submission.getRevisionCount());
    }
}