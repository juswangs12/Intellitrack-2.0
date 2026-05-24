package com.intellitrack.service;

import com.intellitrack.dto.ActivityFeedItemDto;
import com.intellitrack.dto.DeliverableSummaryRowDto;
import com.intellitrack.dto.RawSubmissionSummaryData;
import com.intellitrack.entity.Deliverable;
import com.intellitrack.entity.ProjectGroup;
import com.intellitrack.entity.StudentEnrollment;
import com.intellitrack.entity.Submission;
import com.intellitrack.entity.SubmissionStatus;
import com.intellitrack.repository.DeliverableRepository;
import com.intellitrack.repository.ProjectGroupRepository;
import com.intellitrack.repository.SubmissionRepository;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Service
@Transactional(readOnly = true)
public class SubmissionDataService {

        private final ProjectGroupRepository projectGroupRepository;
        private final SubmissionRepository submissionRepository;
        private final DeliverableRepository deliverableRepository;
        private final StatusEvaluationService statusEvaluationService;

        public SubmissionDataService(
                        ProjectGroupRepository projectGroupRepository,
                        SubmissionRepository submissionRepository,
                        DeliverableRepository deliverableRepository,
                        StatusEvaluationService statusEvaluationService) {
                this.projectGroupRepository = projectGroupRepository;
                this.submissionRepository = submissionRepository;
                this.deliverableRepository = deliverableRepository;
                this.statusEvaluationService = statusEvaluationService;
        }

        @Cacheable(value = "submissionSummaries", key = "#groupId")
        public RawSubmissionSummaryData fetchByGroupId(Long groupId) {
                ProjectGroup group = projectGroupRepository.findById(groupId)
                                .orElseThrow(() -> new IllegalArgumentException("Group not found: " + groupId));

                // Get relevant subject and section IDs from group's student enrollments
                List<Long> subjectIds = new ArrayList<>();
                List<Long> sectionIds = new ArrayList<>();
                List<StudentEnrollment> students = group.getStudents();
                if (students != null) {
                        for (StudentEnrollment enrollment : students) {
                                if (enrollment.getClassSection() != null) {
                                        if (enrollment.getClassSection().getId() != null) {
                                                sectionIds.add(enrollment.getClassSection().getId());
                                        }
                                        if (enrollment.getClassSection().getSubject() != null
                                                        && enrollment.getClassSection().getSubject().getId() != null) {
                                                subjectIds.add(enrollment.getClassSection().getSubject().getId());
                                        }
                                }
                        }
                }

                // Only get active deliverables relevant to the group!
                List<Deliverable> activeDeliverables;
                if (!subjectIds.isEmpty() || !sectionIds.isEmpty()) {
                        activeDeliverables = deliverableRepository.findActiveBySubjectIdsOrSectionIds(subjectIds,
                                        sectionIds);
                } else {
                        activeDeliverables = deliverableRepository.findByActiveTrue();
                }

                // Fetch active deliverables

                List<DeliverableSummaryRowDto> deliverables = activeDeliverables.stream()
                                .map(deliverable -> toSummaryRow(group, deliverable))
                                .toList();

                List<ActivityFeedItemDto> timeline = submissionRepository.findByGroupId(groupId).stream()
                                .sorted(Comparator.comparing(
                                                submission -> submission.getSubmittedAt() == null ? LocalDateTime.MIN
                                                                : submission.getSubmittedAt(),
                                                Comparator.reverseOrder()))
                                .map(submission -> {
                                        LocalDateTime dueAt = submission.getDeliverable().getDueAt();
                                        return new ActivityFeedItemDto(
                                                        submission.getDeliverable().getName() + " "
                                                                        + submission.getStatus().name().toLowerCase(),
                                                        buildTimelineDetail(submission),
                                                        buildTimelineTimestamp(submission.getSubmittedAt(), dueAt));
                                })
                                .toList();

                return new RawSubmissionSummaryData(group.getId(), group.getCode(), group.getTitle(), deliverables,
                                timeline);
        }

        private DeliverableSummaryRowDto toSummaryRow(ProjectGroup group, Deliverable deliverable) {
                Submission submission = submissionRepository
                                .findByGroupIdAndDeliverableId(group.getId(), deliverable.getId())
                                .orElse(null);
                SubmissionStatus status = statusEvaluationService.computeStatus(submission, deliverable.getDueAt());

                return new DeliverableSummaryRowDto(
                                deliverable.getId(),
                                deliverable.getName(),
                                deliverable.getStage(),
                                status.name(),
                                submission == null ? null : submission.getSubmittedAt(),
                                0);
        }

        private String buildTimelineDetail(Submission submission) {
                if (submission.getNotes() != null && !submission.getNotes().isBlank()) {
                        return submission.getNotes();
                }
                return "Submission recorded";
        }

        private String buildTimelineTimestamp(LocalDateTime submittedAt, LocalDateTime dueAt) {
                if (submittedAt != null) {
                        long hoursAgo = Math.max(1, Duration.between(submittedAt, LocalDateTime.now()).toHours());
                        return hoursAgo < 24 ? hoursAgo + "h ago" : (hoursAgo / 24) + "d ago";
                }
                if (dueAt != null) {
                        long hoursRemaining = Duration.between(LocalDateTime.now(), dueAt).toHours();
                        return hoursRemaining >= 0 ? hoursRemaining + "h remaining"
                                        : Math.abs(hoursRemaining) + "h overdue";
                }
                return "No timeline";
        }
}