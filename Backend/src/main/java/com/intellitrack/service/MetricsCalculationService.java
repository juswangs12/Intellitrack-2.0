package com.intellitrack.service;

import com.intellitrack.dto.ActivityFeedItemDto;
import com.intellitrack.dto.GroupProgressDto;
import com.intellitrack.dto.TrackingSnapshotDto;
import com.intellitrack.dto.coordinator.AdviserWorkloadDto;
import com.intellitrack.dto.coordinator.CoordinatorDashboardDto;
import com.intellitrack.dto.coordinator.GroupRiskDto;
import com.intellitrack.entity.Deliverable;
import com.intellitrack.entity.ProjectGroup;
import com.intellitrack.entity.Submission;
import com.intellitrack.entity.SubmissionStatus;
import com.intellitrack.entity.User;
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

@SuppressWarnings("null")
@Service
@Transactional(readOnly = true)
public class MetricsCalculationService {

    private final SubmissionRepository submissionRepository;
    private final DeliverableRepository deliverableRepository;
    private final ProjectGroupRepository projectGroupRepository;
    private final UserRepository userRepository;
    private final StatusEvaluationService statusEvaluationService;
    private final AiService aiService;

    public MetricsCalculationService(
            SubmissionRepository submissionRepository,
            DeliverableRepository deliverableRepository,
            ProjectGroupRepository projectGroupRepository,
            UserRepository userRepository,
            StatusEvaluationService statusEvaluationService,
            AiService aiService) {
        this.submissionRepository = submissionRepository;
        this.deliverableRepository = deliverableRepository;
        this.projectGroupRepository = projectGroupRepository;
        this.userRepository = userRepository;
        this.statusEvaluationService = statusEvaluationService;
        this.aiService = aiService;
    }

    public CoordinatorDashboardDto buildCoordinatorDashboard() {
        List<ProjectGroup> allGroups = projectGroupRepository.findAll();
        List<Submission> allSubmissions = submissionRepository.findAll();
        // Normalize legacy SUBMITTED status in memory to avoid comparison mismatches
        allSubmissions.forEach(s -> {
            if (s.getStatus() != null)
                s.setStatus(s.getStatus().normalize());
        });

        // 1. Submission Stats
        Map<String, Long> stats = new HashMap<>();
        stats.put("total", (long) allSubmissions.size());
        stats.put("onTime",
                allSubmissions.stream().filter(s -> s.getStatus() == SubmissionStatus.SUBMITTED_ON_TIME).count());
        stats.put("late",
                allSubmissions.stream().filter(s -> s.getStatus() == SubmissionStatus.SUBMITTED_LATE).count());
        stats.put("notSubmitted",
                allSubmissions.stream().filter(s -> s.getStatus() == SubmissionStatus.NOT_SUBMITTED).count());
        stats.put("pending",
                allSubmissions.stream().filter(s -> s.getStatus() == SubmissionStatus.PENDING)
                        .count());

        // 2. Adviser Workload
        List<User> advisers = userRepository.findByRole("adviser");
        List<AdviserWorkloadDto> workload = advisers.stream().map(adviser -> {
            List<ProjectGroup> assignedGroups = projectGroupRepository.findByAdviserId(adviser.getId());
            long totalForAdviser = allSubmissions.stream()
                    .filter(s -> assignedGroups.stream().anyMatch(g -> g.getId().equals(s.getGroup().getId()))).count();
            long onTimeForAdviser = allSubmissions.stream()
                    .filter(s -> assignedGroups.stream().anyMatch(g -> g.getId().equals(s.getGroup().getId()))
                            && s.getStatus() == SubmissionStatus.SUBMITTED_ON_TIME)
                    .count();
            double completionRate = (assignedGroups.isEmpty() || totalForAdviser == 0)
                    ? 0.0
                    : Math.round((onTimeForAdviser * 100.0 / totalForAdviser) * 10.0) / 10.0;
            return new AdviserWorkloadDto(
                    adviser.getId(),
                    adviser.getFirstName() + " " + adviser.getLastName(),
                    assignedGroups.size(),
                    totalForAdviser,
                    completionRate);
        }).collect(Collectors.toList());

        // 3. Risk Detection
        List<GroupRiskDto> risks = new ArrayList<>();
        for (ProjectGroup group : allGroups) {
            long lateCount = allSubmissions.stream()
                    .filter(s -> s.getGroup().equals(group) && s.getStatus() == SubmissionStatus.SUBMITTED_LATE)
                    .count();
            if (lateCount > 0) {
                risks.add(new GroupRiskDto(group.getId(), group.getTitle(), lateCount > 2 ? "HIGH" : "MEDIUM",
                        lateCount + " late submissions", (int) lateCount));
            }
        }

        // 4. AI Strategic Insight
        StringBuilder promptBuilder = new StringBuilder();
        promptBuilder.append("You are a Coordinator AI for IntelliTrack capstone tracking. ");
        promptBuilder.append("Summarize the current submission status per subject and section. ");
        promptBuilder
                .append("For each section, also note which students submitted on time, submitted late, or didn't submit.\n\n");

        Map<String, StringBuilder> sectionSummaryMap = new java.util.LinkedHashMap<>();
        for (ProjectGroup group : allGroups) {
            var students = group.getStudents();
            if (students == null || students.isEmpty())
                continue;

            var firstStudent = students.get(0);
            var cs = firstStudent.getClassSection();
            String subjectCode = "Unknown";
            String sectionName = "Unknown";
            if (cs != null) {
                sectionName = cs.getSection() != null ? cs.getSection() : "Unknown";
                if (cs.getSubject() != null)
                    subjectCode = cs.getSubject().getCode() != null ? cs.getSubject().getCode() : "Unknown";
            }
            String sectionKey = subjectCode + " - " + sectionName;
            StringBuilder sb = sectionSummaryMap.computeIfAbsent(sectionKey, k -> new StringBuilder());

            List<Submission> groupSubs = allSubmissions.stream()
                    .filter(s -> s.getGroup() != null && s.getGroup().getId().equals(group.getId()))
                    .collect(Collectors.toList());

            for (var enrollment : students) {
                String studentName = enrollment.getFullName() != null ? enrollment.getFullName()
                        : "Student " + enrollment.getId();
                boolean hasOnTime = groupSubs.stream()
                        .anyMatch(s -> s.getStatus() == SubmissionStatus.SUBMITTED_ON_TIME);
                boolean hasLate = groupSubs.stream().anyMatch(s -> s.getStatus() == SubmissionStatus.SUBMITTED_LATE);
                boolean hasNotSubmitted = groupSubs.stream()
                        .anyMatch(s -> s.getStatus() == SubmissionStatus.NOT_SUBMITTED);

                if (hasOnTime && !hasLate) {
                    sb.append("  - ").append(studentName).append(" submitted on time\n");
                } else if (hasLate && hasOnTime) {
                    sb.append("  - ").append(studentName).append(" submitted late\n");
                } else if (hasLate) {
                    sb.append("  - ").append(studentName).append(" submitted late\n");
                } else if (hasNotSubmitted) {
                    sb.append("  - ").append(studentName).append(" did not submit\n");
                } else {
                    sb.append("  - ").append(studentName).append(" pending\n");
                }
            }
        }

        for (Map.Entry<String, StringBuilder> entry : sectionSummaryMap.entrySet()) {
            promptBuilder.append(entry.getKey()).append(":\n").append(entry.getValue()).append("\n");
        }
        promptBuilder.append(
                "\nProvide a concise strategic summary per subject and section (format: {SubjectCode} - {Section}: {summary}).");

        String prompt = promptBuilder.toString();
        String insight;
        try {
            insight = aiService.getCompletion(prompt).block();
            if (insight == null || insight.isBlank()) {
                insight = "AI insights temporarily unavailable.";
            }
        } catch (Exception e) {
            System.err.println("Coordinator AI insight failed: " + e.getMessage());
            insight = "AI insights temporarily unavailable. " + allGroups.size() + " groups, " + allSubmissions.size()
                    + " submissions tracked.";
        }

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
                SubmissionStatus status = statusEvaluationService.computeStatus(submission, deliverable.getDueAt());

                if (status == SubmissionStatus.SUBMITTED_ON_TIME || status == SubmissionStatus.SUBMITTED_LATE) {
                    submitted++;
                    groupSubmitted++;
                } else if (status == SubmissionStatus.SUBMITTED_LATE) {
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
                        formatTimestamp(submission.getSubmittedAt(), submission.getDeliverable().getDueAt())))
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