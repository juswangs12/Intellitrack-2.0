package com.intellitrack.service;

import com.intellitrack.dto.ActivityFeedItemDto;
import com.intellitrack.dto.GroupProgressDto;
import com.intellitrack.dto.TrackingSnapshotDto;
import com.intellitrack.dto.coordinator.AdviserWorkloadDto;
import com.intellitrack.dto.coordinator.CoordinatorDashboardDto;
import com.intellitrack.dto.coordinator.GroupRiskDto;
import com.intellitrack.entity.Deadline;
import com.intellitrack.entity.Deliverable;
import com.intellitrack.entity.ProjectGroup;
import com.intellitrack.entity.Submission;
import com.intellitrack.entity.SubmissionStatus;
import com.intellitrack.entity.User;
import com.intellitrack.repository.DeadlineRepository;
import com.intellitrack.repository.DeliverableRepository;
import com.intellitrack.repository.ProjectGroupRepository;
import com.intellitrack.repository.SubmissionRepository;
import com.intellitrack.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class MetricsCalculationService {

    private final SubmissionRepository submissionRepository;
    private final DeliverableRepository deliverableRepository;
    private final ProjectGroupRepository projectGroupRepository;
    private final DeadlineRepository deadlineRepository;
    private final UserRepository userRepository;
    private final StatusEvaluationService statusEvaluationService;
    private final AiService aiService;

    public MetricsCalculationService(
            SubmissionRepository submissionRepository,
            DeliverableRepository deliverableRepository,
            ProjectGroupRepository projectGroupRepository,
            DeadlineRepository deadlineRepository,
            UserRepository userRepository,
            StatusEvaluationService statusEvaluationService,
            AiService aiService) {
        this.submissionRepository = submissionRepository;
        this.deliverableRepository = deliverableRepository;
        this.projectGroupRepository = projectGroupRepository;
        this.deadlineRepository = deadlineRepository;
        this.userRepository = userRepository;
        this.statusEvaluationService = statusEvaluationService;
        this.aiService = aiService;
    }

    public CoordinatorDashboardDto buildCoordinatorDashboard() {
        List<ProjectGroup> allGroups = projectGroupRepository.findAll();
        List<Deliverable> allDeliverables = deliverableRepository.findAll();
        List<Submission> allSubmissions = submissionRepository.findAll();

        // 1. Submission Stats
        Map<String, Long> stats = new HashMap<>();
        stats.put("total", (long) allSubmissions.size());
        stats.put("approved", allSubmissions.stream().filter(s -> s.getStatus() == SubmissionStatus.APPROVED).count());
        stats.put("pending", allSubmissions.stream().filter(s -> s.getStatus() == SubmissionStatus.PENDING || s.getStatus() == SubmissionStatus.SUBMITTED).count());
        stats.put("revision", allSubmissions.stream().filter(s -> s.getStatus() == SubmissionStatus.NEEDS_REVISION).count());
        stats.put("late", allSubmissions.stream().filter(s -> s.getStatus() == SubmissionStatus.LATE).count());

        // 2. Adviser Workload
        List<User> advisers = userRepository.findByRole("adviser");
        List<AdviserWorkloadDto> workload = advisers.stream().map(adviser -> {
            List<ProjectGroup> assignedGroups = projectGroupRepository.findByAdviserId(adviser.getId());
            long pendingReviews = allSubmissions.stream()
                .filter(s -> assignedGroups.contains(s.getGroup()) && (s.getStatus() == SubmissionStatus.SUBMITTED || s.getStatus() == SubmissionStatus.PENDING))
                .count();
            return new AdviserWorkloadDto(
                adviser.getId(),
                adviser.getFirstName() + " " + adviser.getLastName(),
                assignedGroups.size(),
                pendingReviews,
                assignedGroups.isEmpty() ? 0 : 100.0 // Placeholder
            );
        }).collect(Collectors.toList());

        // 3. Risk Detection
        List<GroupRiskDto> risks = new ArrayList<>();
        for (ProjectGroup group : allGroups) {
            long lateCount = allSubmissions.stream()
                .filter(s -> s.getGroup().equals(group) && s.getStatus() == SubmissionStatus.LATE)
                .count();
            if (lateCount > 0) {
                risks.add(new GroupRiskDto(group.getId(), group.getTitle(), lateCount > 2 ? "HIGH" : "MEDIUM", lateCount + " late submissions", (int)lateCount));
            }
        }

        // 4. AI Strategic Insight
        String prompt = String.format(
            "As a Coordinator for IntelliTrack, provide a strategic overview of the current status: %d groups, %d submissions, %d late items. Identify potential bottlenecks.",
            allGroups.size(), allSubmissions.size(), stats.get("late")
        );
        String insight = aiService.getCompletion(prompt).block();

        return new CoordinatorDashboardDto(stats, workload, risks, insight);
    }

    public TrackingSnapshotDto buildTrackingSnapshot(Long adviserId) {
        List<ProjectGroup> groups = adviserId == null
                ? projectGroupRepository.findAll()
                : projectGroupRepository.findByAdviserId(adviserId);

        List<Deliverable> deliverables = deliverableRepository.findAll();
        long totalDeliverables = deliverables.size();
        long submitted = 0;
        long pending = 0;
        long late = 0;
        List<GroupProgressDto> progressItems = new ArrayList<>();

        for (ProjectGroup group : groups) {
            long groupSubmitted = 0;

            for (Deliverable deliverable : deliverables) {
                Submission submission = submissionRepository
                        .findByGroupIdAndDeliverableId(group.getId(), deliverable.getId()).orElse(null);
                Deadline deadline = deadlineRepository.findByDeliverableId(deliverable.getId()).orElse(null);
                SubmissionStatus status = statusEvaluationService.computeStatus(submission, deadline);

                if (status == SubmissionStatus.SUBMITTED || status == SubmissionStatus.UPDATED) {
                    submitted++;
                    groupSubmitted++;
                } else if (status == SubmissionStatus.LATE) {
                    late++;
                } else {
                    pending++;
                }
            }

            double completionRate = totalDeliverables == 0
                    ? 0
                    : ((double) groupSubmitted / (double) totalDeliverables) * 100.0;

            progressItems.add(new GroupProgressDto(
                    group.getId(),
                    group.getCode(),
                    group.getTitle(),
                    groupSubmitted,
                    totalDeliverables,
                    roundToOneDecimal(completionRate)));
        }

        List<ActivityFeedItemDto> activityFeed = submissionRepository.findAll().stream()
                .sorted(Comparator.comparing(
                        submission -> Optional.ofNullable(submission.getSubmittedAt()).orElse(LocalDateTime.MIN),
                        Comparator.reverseOrder()))
                .limit(6)
                .map(submission -> new ActivityFeedItemDto(
                        submission.getDeliverable().getName() + " updated",
                        submission.getGroup().getTitle() + " is currently " + submission.getStatus().name(),
                        formatTimestamp(submission.getSubmittedAt(),
                                deadlineRepository.findByDeliverableId(submission.getDeliverable().getId())
                                        .map(Deadline::getDueAt).orElse(null))))
                .toList();

        return new TrackingSnapshotDto(totalDeliverables, submitted, pending, late, activityFeed, progressItems);
    }

    private String formatTimestamp(LocalDateTime submittedAt, LocalDateTime dueAt) {
        if (submittedAt != null) {
            long hoursAgo = Math.max(1, Duration.between(submittedAt, LocalDateTime.now()).toHours());
            return hoursAgo < 24 ? hoursAgo + "h ago" : (hoursAgo / 24) + "d ago";
        }

        if (dueAt != null) {
            long hoursRemaining = Duration.between(LocalDateTime.now(), dueAt).toHours();
            return hoursRemaining >= 0 ? hoursRemaining + "h remaining" : Math.abs(hoursRemaining) + "h overdue";
        }

        return "No timeline";
    }

    private double roundToOneDecimal(double value) {
        return Math.round(value * 10.0) / 10.0;
    }
}