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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
        List<Deliverable> deliverables = deliverableRepository.findAll();
        List<Long> deliverableIds = deliverables.stream().map(Deliverable::getId).toList();
        
        List<Deadline> allDeadlines = deadlineRepository.findByDeliverableIdIn(deliverableIds);
        Map<Long, Deadline> deadlinesByDeliverableId = new HashMap<>();
        for (Deadline deadline : allDeadlines) {
            if (deadline.getDeliverable() != null && deadline.getDeliverable().getId() != null) {
                deadlinesByDeliverableId.put(deadline.getDeliverable().getId(), deadline);
            }
        }
        
        List<DeliverableStatusDto> statuses = new ArrayList<>();
        for (Deliverable deliverable : deliverables) {
            Deadline deadline = deadlinesByDeliverableId.get(deliverable.getId());
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

        List<Deliverable> deliverables = deliverableRepository.findAll();
        long totalDeliverables = deliverables.size();
        
        List<Long> deliverableIds = deliverables.stream().map(Deliverable::getId).toList();
        List<Deadline> allDeadlines = deadlineRepository.findByDeliverableIdIn(deliverableIds);
        Map<Long, Deadline> deadlinesByDeliverableId = new HashMap<>();
        for (Deadline deadline : allDeadlines) {
            if (deadline.getDeliverable() != null && deadline.getDeliverable().getId() != null) {
                deadlinesByDeliverableId.put(deadline.getDeliverable().getId(), deadline);
            }
        }
        
        return groups.stream().map(group -> {
            List<Submission> submissions = submissionRepository.findByGroupId(group.getId());
            long onTime = submissions.stream()
                    .filter(submission -> computeStatus(submission,
                            deadlinesByDeliverableId.get(submission.getDeliverable().getId())) == SubmissionStatus.SUBMITTED_ON_TIME)
                    .count();
            long late = submissions.stream().filter(submission -> computeStatus(submission, 
                    deadlinesByDeliverableId.get(submission.getDeliverable().getId())) == SubmissionStatus.SUBMITTED_LATE)
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

    public SubmissionStatus computeStatus(Submission submission, Deadline deadline) {
        if (submission == null) {
            if (deadline == null) {
                return SubmissionStatus.PENDING;
            }
            return deadline.getDueAt().isBefore(LocalDateTime.now()) ? SubmissionStatus.NOT_SUBMITTED : SubmissionStatus.PENDING;
        }

        if (submission.getSubmittedAt() == null) {
            if (deadline == null) {
                return SubmissionStatus.PENDING;
            }
            return deadline.getDueAt().isBefore(LocalDateTime.now()) ? SubmissionStatus.NOT_SUBMITTED : SubmissionStatus.PENDING;
        }

        if (deadline == null) {
            return SubmissionStatus.SUBMITTED_ON_TIME;
        }

        if (submission.getSubmittedAt().isAfter(deadline.getDueAt())) {
            return SubmissionStatus.SUBMITTED_LATE;
        }

        return SubmissionStatus.SUBMITTED_ON_TIME;
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
                0);
    }
}
