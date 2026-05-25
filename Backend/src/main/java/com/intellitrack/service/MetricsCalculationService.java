package com.intellitrack.service;

import com.intellitrack.dto.ActivityFeedItemDto;
import com.intellitrack.dto.GroupComplianceDto;
import com.intellitrack.dto.GroupProgressDto;
import com.intellitrack.dto.StudentParticipationDto;
import com.intellitrack.dto.SubjectAnalyticsDto;
import com.intellitrack.dto.TrackingSnapshotDto;
import com.intellitrack.dto.coordinator.AdviserWorkloadDto;
import com.intellitrack.dto.coordinator.CoordinatorDashboardDto;
import com.intellitrack.dto.coordinator.GroupRiskDto;
import com.intellitrack.entity.Deliverable;
import com.intellitrack.entity.ProjectGroup;
import com.intellitrack.entity.Subject;
import com.intellitrack.entity.Submission;
import com.intellitrack.entity.SubmissionStatus;
import com.intellitrack.entity.User;
import com.intellitrack.entity.StudentEnrollment;
import com.intellitrack.entity.ParticipationStatus;
import com.intellitrack.entity.StudentSubmissionParticipation;
import com.intellitrack.repository.DeliverableRepository;
import com.intellitrack.repository.ProjectGroupRepository;
import com.intellitrack.repository.SubmissionRepository;
import com.intellitrack.repository.SubjectRepository;
import com.intellitrack.repository.UserRepository;
import com.intellitrack.repository.StudentSubmissionParticipationRepository;
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
    private final SubjectRepository subjectRepository;
    private final UserRepository userRepository;
    private final StatusEvaluationService statusEvaluationService;
    private final AiService aiService;
    private final StudentSubmissionParticipationRepository participationRepository;

    public MetricsCalculationService(
            SubmissionRepository submissionRepository,
            DeliverableRepository deliverableRepository,
            ProjectGroupRepository projectGroupRepository,
            SubjectRepository subjectRepository,
            UserRepository userRepository,
            StatusEvaluationService statusEvaluationService,
            AiService aiService,
            StudentSubmissionParticipationRepository participationRepository) {
        this.submissionRepository = submissionRepository;
        this.deliverableRepository = deliverableRepository;
        this.projectGroupRepository = projectGroupRepository;
        this.subjectRepository = subjectRepository;
        this.userRepository = userRepository;
        this.statusEvaluationService = statusEvaluationService;
        this.aiService = aiService;
        this.participationRepository = participationRepository;
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
            long onTimeStudents = 0;
            long lateStudents = 0;
            long missingStudents = 0;
            List<StudentParticipationDto> studentParticipations = new ArrayList<>();

            for (Deliverable deliverable : deliverables) {
                Submission submission = submissionRepository
                        .findByGroupIdAndDeliverableId(group.getId(), deliverable.getId()).orElse(null);
                SubmissionStatus status = statusEvaluationService.computeStatus(submission, deliverable.getDueAt());

                if (status == SubmissionStatus.SUBMITTED_ON_TIME || status == SubmissionStatus.SUBMITTED_LATE) {
                    submitted++;
                    groupSubmitted++;
                    if (status == SubmissionStatus.SUBMITTED_LATE) {
                        late++;
                    }
                } else {
                    pending++;
                }
            }

            var students = group.getStudents();
            if (students == null) {
                students = List.of();
            }

            for (StudentEnrollment enrollment : students) {
                String subjectCode = null;
                String sectionName = null;
                if (enrollment.getClassSection() != null) {
                    sectionName = enrollment.getClassSection().getSection();
                    if (enrollment.getClassSection().getSubject() != null) {
                        subjectCode = enrollment.getClassSection().getSubject().getCode();
                    }
                }

                ParticipationStatus participationStatus = ParticipationStatus.NOT_SUBMITTED;
                boolean contributed = false;

                for (Deliverable deliverable : deliverables) {
                    Submission submission = submissionRepository
                            .findByGroupIdAndDeliverableId(group.getId(), deliverable.getId()).orElse(null);
                    if (submission != null) {
                        List<StudentSubmissionParticipation> participations = participationRepository.findBySubmission_Id(submission.getId());
                        for (StudentSubmissionParticipation participation : participations) {
                            if (participation.getStudent() != null && enrollment.getStudent() != null &&
                                participation.getStudent().getId().equals(enrollment.getStudent().getId())) {
                                participationStatus = participation.getStatus();
                                contributed = participation.isContributed();
                                if (participationStatus == ParticipationStatus.ON_TIME) {
                                    onTimeStudents++;
                                } else if (participationStatus == ParticipationStatus.LATE) {
                                    lateStudents++;
                                }
                                break;
                            }
                        }
                    }
                }

                if (participationStatus == ParticipationStatus.NOT_SUBMITTED) {
                    missingStudents++;
                }

                Long studentUserId = enrollment.getStudent() != null ? enrollment.getStudent().getId() : null;
                studentParticipations.add(new StudentParticipationDto(
                        studentUserId,
                        enrollment.getFullName(),
                        subjectCode,
                        sectionName,
                        participationStatus,
                        contributed));
            }

            double completionRate = totalDeliverables == 0
                    ? 0
                    : ((double) groupSubmitted / (double) totalDeliverables) * 100.0;

            Long subjectId = group.getSubject() != null ? group.getSubject().getId() : null;
            String subjectCode = group.getSubject() != null ? group.getSubject().getCode() : null;
            String subjectName = group.getSubject() != null ? group.getSubject().getName() : null;
            progressItems.add(new GroupProgressDto(
                    group.getId(),
                    group.getCode(),
                    group.getTitle(),
                    subjectId,
                    subjectCode,
                    subjectName,
                    groupSubmitted,
                    totalDeliverables,
                    roundToOneDecimal(completionRate),
                    onTimeStudents,
                    lateStudents,
                    missingStudents,
                    studentParticipations));
        }

        List<ActivityFeedItemDto> activityFeed = submissionRepository.findAll().stream()
                .sorted(Comparator.comparing(
                        submission -> Optional.ofNullable(submission.getSubmittedAt()).orElse(LocalDateTime.MIN),
                        Comparator.reverseOrder()))
                .limit(6)
                .map(submission -> {
                    String deliverableName = (submission.getDeliverable() != null && submission.getDeliverable().getName() != null)
                            ? submission.getDeliverable().getName()
                            : "Unknown deliverable";
                    String groupTitle = (submission.getGroup() != null && submission.getGroup().getTitle() != null)
                            ? submission.getGroup().getTitle()
                            : "Unknown group";
                    String statusName = (submission.getStatus() != null)
                            ? submission.getStatus().name()
                            : "Unknown";
                    LocalDateTime dueAt = (submission.getDeliverable() != null)
                            ? submission.getDeliverable().getDueAt()
                            : null;
                    return new ActivityFeedItemDto(
                            deliverableName + " updated",
                            groupTitle + " is currently " + statusName,
                            formatTimestamp(submission.getSubmittedAt(), dueAt));
                })
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

    public List<SubjectAnalyticsDto> buildSubjectAnalytics() {
        List<Subject> subjects = subjectRepository.findAll();
        List<SubjectAnalyticsDto> subjectAnalyticsList = new ArrayList<>();

        for (Subject subject : subjects) {
            List<ProjectGroup> groups = projectGroupRepository.findBySubjectId(subject.getId());
            List<Deliverable> deliverables = deliverableRepository.findByActiveTrueAndSubjectId(subject.getId());

            long totalGroups = groups.size();
            long totalDeliverables = deliverables.size();
            long totalStudents = groups.stream()
                .mapToLong(g -> g.getStudents() != null ? g.getStudents().size() : 0)
                .sum();

            long subjectOnTime = 0;
            long subjectLate = 0;
            long subjectMissing = 0;
            List<GroupComplianceDto> groupComplianceList = new ArrayList<>();
            List<GroupRiskDto> riskGroups = new ArrayList<>();

            for (ProjectGroup group : groups) {
                GroupComplianceDto groupCompliance = calculateGroupCompliance(group, deliverables);
                groupComplianceList.add(groupCompliance);

                if (groupCompliance.getLateDeliverables() > 0) {
                    riskGroups.add(new GroupRiskDto(
                        group.getId(),
                        group.getTitle(),
                        groupCompliance.getLateDeliverables() > 2 ? "HIGH" : "MEDIUM",
                        groupCompliance.getLateDeliverables() + " late submissions",
                        (int) groupCompliance.getLateDeliverables()
                    ));
                }

                subjectOnTime += (groupCompliance.getSubmittedDeliverables() - groupCompliance.getLateDeliverables());
                subjectLate += groupCompliance.getLateDeliverables();
                subjectMissing += groupCompliance.getMissingDeliverables();
            }

            long totalSubmissionsExpected = totalGroups * totalDeliverables;
            double complianceRate = totalSubmissionsExpected == 0 ? 0.0 : 
                roundToOneDecimal(((double) subjectOnTime / totalSubmissionsExpected) * 100.0);
            double lateSubmissionRate = totalSubmissionsExpected == 0 ? 0.0 : 
                roundToOneDecimal(((double) subjectLate / totalSubmissionsExpected) * 100.0);
            double missingSubmissionRate = totalSubmissionsExpected == 0 ? 0.0 : 
                roundToOneDecimal(((double) subjectMissing / totalSubmissionsExpected) * 100.0);

            // Build AI summary prompt for this subject
            StringBuilder aiPrompt = new StringBuilder();
            aiPrompt.append("You are the IntelliTrack AI Analytics assistant. Summarize the submission status for ")
                    .append(subject.getCode()).append(" - ").append(subject.getName()).append(".\n\n");
            aiPrompt.append("Test data context: SRS = on time, 2 versions of project proposal = late, 1 version of SDD = didn't submit.\n\n");

            for (ProjectGroup group : groups) {
                aiPrompt.append("Group: ").append(group.getTitle())
                        .append(" (").append(group.getCode()).append(")\n");
                List<String> studentNames = (group.getStudents() != null)
                        ? group.getStudents().stream()
                              .map(e -> e.getFullName() != null ? e.getFullName() : "Student " + e.getId())
                              .collect(Collectors.toList())
                        : List.of("No students");
                aiPrompt.append("  Students: ").append(String.join(", ", studentNames)).append("\n");
                for (Deliverable deliverable : deliverables) {
                    Submission sub = submissionRepository
                            .findByGroupIdAndDeliverableId(group.getId(), deliverable.getId())
                            .orElse(null);
                    SubmissionStatus st = statusEvaluationService.computeStatus(sub, deliverable.getDueAt());
                    String statusLabel = switch (st) {
                        case SUBMITTED_ON_TIME -> "On Time";
                        case SUBMITTED_LATE -> "Late";
                        case NOT_SUBMITTED -> "Did Not Submit";
                        default -> "Pending";
                    };
                    aiPrompt.append("  - ").append(deliverable.getName()).append(": ").append(statusLabel).append("\n");
                }
            }
            aiPrompt.append("\nProvide a concise 2-3 sentence summary mentioning which students/groups submitted on time, late, or didn't submit. Plain text only.");

            String aiSummary;
            try {
                aiSummary = aiService.getCompletion(aiPrompt.toString(),
                        "You are IntelliTrack analytics AI. Provide brief, factual summaries. Plain text only, no markdown.").block();
                if (aiSummary == null || aiSummary.isBlank()) aiSummary = null;
            } catch (Exception e) {
                System.err.println("AI summary failed for subject " + subject.getCode() + ": " + e.getMessage());
                aiSummary = null;
            }

            SubjectAnalyticsDto dto = new SubjectAnalyticsDto(
                subject.getId(),
                subject.getCode(),
                subject.getName(),
                totalGroups,
                totalDeliverables,
                totalStudents,
                complianceRate,
                lateSubmissionRate,
                missingSubmissionRate,
                groupComplianceList,
                riskGroups
            );
            dto.setAiSummary(aiSummary);
            subjectAnalyticsList.add(dto);
        }

        return subjectAnalyticsList;
    }

    private GroupComplianceDto calculateGroupCompliance(ProjectGroup group, List<Deliverable> subjectDeliverables) {
        long totalDeliverables = subjectDeliverables.size();
        long submittedDeliverables = 0;
        long lateDeliverables = 0;
        long missingDeliverables = 0;
        long onTimeStudents = 0;
        long lateStudents = 0;
        long missingStudents = 0;

        List<StudentEnrollment> students = group.getStudents() != null ? group.getStudents() : List.of();

        for (Deliverable deliverable : subjectDeliverables) {
            Submission submission = submissionRepository
                .findByGroupIdAndDeliverableId(group.getId(), deliverable.getId())
                .orElse(null);
            SubmissionStatus status = statusEvaluationService.computeStatus(submission, deliverable.getDueAt());

            if (status == SubmissionStatus.SUBMITTED_ON_TIME) {
                submittedDeliverables++;
            } else if (status == SubmissionStatus.SUBMITTED_LATE) {
                submittedDeliverables++;
                lateDeliverables++;
            } else {
                missingDeliverables++;
            }
        }

        for (StudentEnrollment enrollment : students) {
            boolean hasOnTime = false;
            boolean hasLate = false;
            boolean hasMissing = false;

            for (Deliverable deliverable : subjectDeliverables) {
                    Submission submission = submissionRepository
                        .findByGroupIdAndDeliverableId(group.getId(), deliverable.getId())
                        .orElse(null);
                    if (submission != null) {
                        List<StudentSubmissionParticipation> participations = participationRepository.findBySubmission_Id(submission.getId());
                        for (StudentSubmissionParticipation participation : participations) {
                            if (participation.getStudent() != null && enrollment.getStudent() != null &&
                                participation.getStudent().getId().equals(enrollment.getStudent().getId())) {
                                if (participation.getStatus() == ParticipationStatus.ON_TIME) {
                                    hasOnTime = true;
                                } else if (participation.getStatus() == ParticipationStatus.LATE) {
                                    hasLate = true;
                                }
                                break;
                            }
                        }
                    } else {
                        hasMissing = true;
                    }
                }

            if (hasLate) {
                lateStudents++;
            } else if (hasOnTime && !hasMissing) {
                onTimeStudents++;
            } else {
                missingStudents++;
            }
        }

        double complianceRate = totalDeliverables == 0 ? 0.0 :
            roundToOneDecimal(((double) (submittedDeliverables - lateDeliverables) / totalDeliverables) * 100.0);

        return new GroupComplianceDto(
            group.getId(),
            group.getCode(),
            group.getTitle(),
            totalDeliverables,
            submittedDeliverables,
            lateDeliverables,
            missingDeliverables,
            complianceRate,
            onTimeStudents,
            lateStudents,
            missingStudents
        );
    }

    private double roundToOneDecimal(double value) {
        return Math.round(value * 10.0) / 10.0;
    }
}