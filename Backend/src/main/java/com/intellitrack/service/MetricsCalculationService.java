package com.intellitrack.service;

import com.intellitrack.dto.ActivityFeedItemDto;
import com.intellitrack.dto.GroupComplianceDto;
import com.intellitrack.dto.MissingDeliverableDetailDto;
import com.intellitrack.dto.OnTimeDeliverableDetailDto;
import com.intellitrack.dto.LateDeliverableDetailDto;
import com.intellitrack.dto.GroupProgressDto;
import com.intellitrack.dto.StudentAnalyticsDto;
import com.intellitrack.dto.StudentDeliverableStatusDto;
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
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@SuppressWarnings("null")
@Service
@Transactional(readOnly = true)
public class MetricsCalculationService {

    private static final String ANALYTICS_SUMMARY_SYSTEM_PROMPT = """
        You are an AI analytics engine for a Capstone Submission Tracking System (IntelliTrack). 
 
        Your job is to generate STRICTLY structured, clear, and actionable summaries for the Coordinator role.
 
        You MUST follow the formatting rules exactly. Do not be creative with structure. Do not write long paragraphs unless instructed. 

        GENERAL RULES (VERY IMPORTANT):
        - Use FULL student names (never first name only, never partial names)
        - Be factual and structured based on provided data only
        - Do NOT hallucinate missing data
        - Always classify submissions as: ON TIME, LATE, MISSING
        - Always mention deliverable names explicitly
        - Keep language professional and analytical
        - Avoid storytelling or vague summaries
        - NO emojis
        - NO fluff
        - NO motivational phrases
        - NO vague statements like "doing well" without data

        COORDINATOR ANALYTICS OUTPUT FORMAT:

        ### Overall System Summary
        - Total Groups: {number}
        - Overall Submission Performance:
          - On Time: {count} ({percentage}%)
          - Late: {count} ({percentage}%)
          - Missing: {count} ({percentage}%)

        ---

        ### Group Breakdown

        For EACH group:

        #### Group: {Group Name}
        - Progress Status: (Healthy / At Risk / Critical based on submission completion rate)

        - Submissions Summary:
          - On Time: {count}
          - Late: {count}
          - Missing: {count}

        - Submitted Students:
          - {Full Name} → Submitted: {Deliverable Name(s)} (Status: On Time/Late)

        - Non-Submitting Students:
          - {Full Name} → Missing: {Deliverable Name(s)}

        - Risk Insight:
          - Short 1–2 sentence explanation of group performance

        ---

        ### System-Level Insight
        - Overall evaluation of all groups
        - Identify most at-risk group
        - Identify top performing group

        Return ONLY the formatted output. No extra explanation.
        """;

    private static final long AI_CACHE_TTL_SECONDS = 300;
    private static final long AI_MIN_COOLDOWN_SECONDS = 60;

    private record CachedInsight(String insight, Instant cachedAt, Instant lastGeneratedAt) {}

    private volatile CachedInsight coordinatorInsightCache = null;

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
        allSubmissions.forEach(s -> {
            if (s.getStatus() != null)
                s.setStatus(s.getStatus().normalize());
        });

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

        String insight = allGroups.size() + " groups, " + allSubmissions.size() + " submissions tracked.";

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
            long groupOverdue = 0;
            long onTimeStudents = 0;
            long lateStudents = 0;
            long missingStudents = 0;
            List<StudentParticipationDto> studentParticipations = new ArrayList<>();

            for (Deliverable deliverable : deliverables) {
                Submission submission = submissionRepository
                        .findByGroupIdAndDeliverableId(group.getId(), deliverable.getId()).orElse(null);
                SubmissionStatus status = statusEvaluationService.computeStatus(submission, deliverable.getDueAt());

                if (status == SubmissionStatus.SUBMITTED_ON_TIME
                        || status == SubmissionStatus.SUBMITTED_LATE) {

                    submitted++;
                    groupSubmitted++;

                    if (status == SubmissionStatus.SUBMITTED_LATE) {
                        late++;
                    }

                }
                else if (status == SubmissionStatus.OVERDUE) {

                    late++;
                    groupOverdue++;

                }
                else {

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
                        List<StudentSubmissionParticipation> participations = participationRepository
                                .findBySubmission_Id(submission.getId());
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
                groupOverdue,                
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
                    String deliverableName = (submission.getDeliverable() != null
                            && submission.getDeliverable().getName() != null)
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
                            (int) groupCompliance.getLateDeliverables()));
                }

                subjectOnTime += (groupCompliance.getSubmittedDeliverables() - groupCompliance.getLateDeliverables());
                subjectLate += groupCompliance.getLateDeliverables();
                subjectMissing += groupCompliance.getMissingDeliverables();
            }

            long totalSubmissionsExpected = totalGroups * totalDeliverables;
            double complianceRate = totalSubmissionsExpected == 0 ? 0.0
                    : roundToOneDecimal(((double) subjectOnTime / totalSubmissionsExpected) * 100.0);
            double lateSubmissionRate = totalSubmissionsExpected == 0 ? 0.0
                    : roundToOneDecimal(((double) subjectLate / totalSubmissionsExpected) * 100.0);
            double missingSubmissionRate = totalSubmissionsExpected == 0 ? 0.0
                    : roundToOneDecimal(((double) subjectMissing / totalSubmissionsExpected) * 100.0);

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
                    riskGroups);
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
                    List<StudentSubmissionParticipation> participations = participationRepository
                            .findBySubmission_Id(submission.getId());
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

        double complianceRate = totalDeliverables == 0 ? 0.0
                : roundToOneDecimal(((double) (submittedDeliverables - lateDeliverables) / totalDeliverables) * 100.0);

        // Build per-deliverable details
        List<MissingDeliverableDetailDto> missingDeliverableDetails = new ArrayList<>();
        List<OnTimeDeliverableDetailDto> onTimeDeliverableDetails = new ArrayList<>();
        List<LateDeliverableDetailDto> lateDeliverableDetails = new ArrayList<>();
        
        for (Deliverable deliverable : subjectDeliverables) {
            List<String> missingStudentNames = new ArrayList<>();
            List<String> onTimeStudentNames = new ArrayList<>();
            List<String> lateStudentNames = new ArrayList<>();
            
            for (StudentEnrollment enrollment : students) {
                if (enrollment.getStudent() == null) continue;
                String studentName = enrollment.getFullName();
                if (studentName == null) continue;
                
                Submission submission = submissionRepository
                        .findByGroupIdAndDeliverableId(group.getId(), deliverable.getId())
                        .orElse(null);
                
                if (submission == null) {
                    missingStudentNames.add(studentName);
                } else {
                    List<StudentSubmissionParticipation> participations = participationRepository
                            .findBySubmission_Id(submission.getId());
                    boolean found = false;
                    for (StudentSubmissionParticipation participation : participations) {
                        if (participation.getStudent() != null && 
                            participation.getStudent().getId().equals(enrollment.getStudent().getId())) {
                            found = true;
                            if (participation.getStatus() == ParticipationStatus.ON_TIME) {
                                onTimeStudentNames.add(studentName);
                            } else if (participation.getStatus() == ParticipationStatus.LATE) {
                                lateStudentNames.add(studentName);
                            }
                            break;
                        }
                    }
                    if (!found) {
                        missingStudentNames.add(studentName);
                    }
                }
            }
            
            if (!missingStudentNames.isEmpty()) {
                missingDeliverableDetails.add(new MissingDeliverableDetailDto(deliverable.getName(), missingStudentNames));
            }
            if (!onTimeStudentNames.isEmpty()) {
                onTimeDeliverableDetails.add(new OnTimeDeliverableDetailDto(deliverable.getName(), onTimeStudentNames));
            }
            if (!lateStudentNames.isEmpty()) {
                lateDeliverableDetails.add(new LateDeliverableDetailDto(deliverable.getName(), lateStudentNames));
            }
        }

        GroupComplianceDto dto = new GroupComplianceDto(
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
                missingStudents);
        dto.setMissingDeliverableDetails(missingDeliverableDetails);
        dto.setOnTimeDeliverableDetails(onTimeDeliverableDetails);
        dto.setLateDeliverableDetails(lateDeliverableDetails);
        return dto;
    }

    private double roundToOneDecimal(double value) {
        return Math.round(value * 10.0) / 10.0;
    }

    @Transactional(readOnly = true)
    public List<StudentAnalyticsDto> buildStudentAnalytics() {
        List<StudentAnalyticsDto> result = new ArrayList<>();
        List<Deliverable> allDeliverables = deliverableRepository.findAll();
        List<StudentSubmissionParticipation> allParticipations = participationRepository.findAll();

        Map<Long, List<StudentSubmissionParticipation>> participationsByStudent = new HashMap<>();
        for (StudentSubmissionParticipation participation : allParticipations) {
            if (participation.getStudent() != null) {
                participationsByStudent
                        .computeIfAbsent(participation.getStudent().getId(), k -> new ArrayList<>())
                        .add(participation);
            }
        }

        for (Map.Entry<Long, List<StudentSubmissionParticipation>> entry : participationsByStudent.entrySet()) {
            Long studentId = entry.getKey();
            List<StudentSubmissionParticipation> participations = entry.getValue();
            
            if (participations.isEmpty()) continue;

            StudentSubmissionParticipation firstParticipation = participations.get(0);
            String studentName = null;
            Long groupId = null;
            String groupCode = null;
            String groupTitle = null;
            String subjectCode = null;
            String subjectName = null;
            String sectionName = null;

            if (firstParticipation.getStudent() != null) {
                studentName = firstParticipation.getStudent().getFirstName() + " " + 
                            firstParticipation.getStudent().getLastName();
            }

            if (firstParticipation.getSubmission() != null && firstParticipation.getSubmission().getGroup() != null) {
                ProjectGroup group = firstParticipation.getSubmission().getGroup();
                groupId = group.getId();
                groupCode = group.getCode();
                groupTitle = group.getTitle();
            }

            if (firstParticipation.getEnrollment() != null && 
                firstParticipation.getEnrollment().getClassSection() != null) {
                sectionName = firstParticipation.getEnrollment().getClassSection().getSection();
                if (firstParticipation.getEnrollment().getClassSection().getSubject() != null) {
                    subjectCode = firstParticipation.getEnrollment().getClassSection().getSubject().getCode();
                    subjectName = firstParticipation.getEnrollment().getClassSection().getSubject().getName();
                }
            }

            Map<Long, StudentSubmissionParticipation> participationsByDeliverable = new HashMap<>();
            for (StudentSubmissionParticipation p : participations) {
                if (p.getSubmission() != null && p.getSubmission().getDeliverable() != null) {
                    participationsByDeliverable.put(p.getSubmission().getDeliverable().getId(), p);
                }
            }

            List<StudentDeliverableStatusDto> deliverableStatuses = new ArrayList<>();
            long onTimeDeliverables = 0;
            long lateDeliverables = 0;
            long missingDeliverables = 0;

            for (Deliverable deliverable : allDeliverables) {
                StudentSubmissionParticipation participation = participationsByDeliverable.get(deliverable.getId());
                
                String status = "NOT_SUBMITTED";
                java.time.LocalDateTime submittedAt = null;
                Integer lateDays = null;

                if (participation != null && participation.getSubmission() != null) {
                    Submission submission = participation.getSubmission();
                    submittedAt = submission.getSubmittedAt();
                    
                    if (submission.getStatus() != null) {
                        status = submission.getStatus().name();
                        
                        if (status.equals("SUBMITTED_ON_TIME")) {
                            onTimeDeliverables++;
                        } else if (status.equals("SUBMITTED_LATE")) {
                            lateDeliverables++;
                        }
                    }

                    if (deliverable.getDueAt() != null && submittedAt != null) {
                        java.time.Duration duration = java.time.Duration.between(deliverable.getDueAt(), submittedAt);
                        long daysLate = duration.toDays();
                        if (daysLate > 0) {
                            lateDays = (int) daysLate;
                        }
                    }
                } else {
                    missingDeliverables++;
                }

                deliverableStatuses.add(new StudentDeliverableStatusDto(
                        deliverable.getId(),
                        deliverable.getName(),
                        deliverable.getStage(),
                        status,
                        submittedAt,
                        lateDays
                ));
            }

            double complianceRate = allDeliverables.isEmpty() ? 0.0 :
                    roundToOneDecimal(((double) onTimeDeliverables / (double) allDeliverables.size()) * 100.0);

            result.add(new StudentAnalyticsDto(
                    studentId,
                    studentName,
                    groupId,
                    groupCode,
                    groupTitle,
                    subjectCode,
                    subjectName,
                    sectionName,
                    allDeliverables.size(),
                    onTimeDeliverables,
                    lateDeliverables,
                    missingDeliverables,
                    complianceRate,
                    deliverableStatuses
            ));
        }

        return result;
    }

    @Transactional(readOnly = true)
    public com.intellitrack.dto.CoordinatorAnalyticsSummaryDto buildCoordinatorAnalyticsSummary() {
        List<SubjectAnalyticsDto> subjects = buildSubjectAnalytics();
        List<StudentAnalyticsDto> students = buildStudentAnalytics();
        String aiSummary = null;
        
        CachedInsight cached = coordinatorInsightCache;
        if (cached != null) {
            aiSummary = cached.insight();
            System.out.println("=== USING CACHED AI ANALYSIS - Coordinator ===");
        } else {
            aiSummary = "Click the 'Generate AI Summary' button to create an AI-powered analytics summary.";
        }
        
        return new com.intellitrack.dto.CoordinatorAnalyticsSummaryDto(
                subjects,
                students,
                aiSummary
        );
    }

    public String generateCoordinatorAiSummary() {
        List<SubjectAnalyticsDto> subjects = buildSubjectAnalytics();
        List<StudentAnalyticsDto> students = buildStudentAnalytics();
        
        CachedInsight cached = coordinatorInsightCache;
        
        try {
            System.out.println("=== GENERATING NEW AI ANALYSIS (EXPLICIT REQUEST) - Coordinator ===");
            
            StringBuilder promptBuilder = new StringBuilder();
            
            promptBuilder.append("COORDINATOR ANALYTICS DATA:\n\n");
            
            promptBuilder.append("SUBJECT AND GROUP BREAKDOWN:\n");
            for (SubjectAnalyticsDto subject : subjects) {
                promptBuilder.append(String.format("\n=== SUBJECT: %s (%s) ===\n",
                        subject.getSubjectName(), subject.getSubjectCode()));
                promptBuilder.append(String.format("- Total Groups: %d\n", subject.getTotalGroups()));
                promptBuilder.append(String.format("- Total Students: %d\n", subject.getTotalStudents()));
                promptBuilder.append(String.format("- Compliance Rate: %.0f%%\n", subject.getComplianceRate()));
                promptBuilder.append(String.format("- Late Rate: %.0f%%\n", subject.getLateSubmissionRate()));
                promptBuilder.append(String.format("- Missing Rate: %.0f%%\n", subject.getMissingSubmissionRate()));
                
                promptBuilder.append("\nALL GROUPS:\n");
                for (var group : subject.getGroupCompliance()) {
                    promptBuilder.append(String.format("\n  Group: %s (%s)\n", group.getGroupTitle(), group.getGroupCode()));
                    promptBuilder.append(String.format("  - Compliance: %.0f%%\n", group.getComplianceRate()));
                    promptBuilder.append(String.format("  - On-Time Deliverables: %d\n", group.getSubmittedDeliverables() - group.getLateDeliverables()));
                    promptBuilder.append(String.format("  - Late Deliverables: %d\n", group.getLateDeliverables()));
                    promptBuilder.append(String.format("  - Missing Deliverables: %d\n", group.getMissingDeliverables()));
                    promptBuilder.append(String.format("  - On-Time Students: %d\n", group.getOnTimeStudents()));
                    promptBuilder.append(String.format("  - Late Students: %d\n", group.getLateStudents()));
                    promptBuilder.append(String.format("  - Missing Students: %d\n", group.getMissingStudents()));
                }
                
                if (subject.getRiskGroups() != null && subject.getRiskGroups().size() > 0) {
                    promptBuilder.append("\nAT-RISK GROUPS:\n");
                    for (var riskGroup : subject.getRiskGroups()) {
                        promptBuilder.append(String.format("  - %s: Risk Level %s, %s\n",
                                riskGroup.groupTitle(), riskGroup.riskLevel(), riskGroup.reason()));
                    }
                }
            }
            
            promptBuilder.append("\n\nSTUDENT SUMMARY:\n");
            for (StudentAnalyticsDto student : students) {
                promptBuilder.append(String.format("- %s (Group: %s, Subject: %s): On-time: %d, Late: %d, Missing: %d, Compliance: %.0f%%\n",
                        student.studentName(), student.groupTitle(), student.subjectCode(),
                        student.onTimeDeliverables(), student.lateDeliverables(), student.missingDeliverables(),
                        student.complianceRate()));
            }
            
            String summary = aiService.getCompletion(promptBuilder.toString(), ANALYTICS_SUMMARY_SYSTEM_PROMPT).block();
            
            if (summary != null && !summary.trim().isEmpty()) {
                coordinatorInsightCache = new CachedInsight(summary, Instant.now(), Instant.now());
                System.out.println("=== AI ANALYSIS SAVED - Coordinator ===");
                return summary;
            } else {
                if (cached != null && cached.insight() != null) {
                    System.out.println("=== USING FALLBACK CACHED AI ANALYSIS - Coordinator ===");
                    return cached.insight();
                }
                return "AI summary is not available. Please configure your AI_API_KEY environment variable to enable AI features.";
            }
        } catch (Exception e) {
            System.err.println("=== AI RATE LIMIT HIT - Coordinator: " + e.getMessage() + " ===");
            
            if (cached != null && cached.insight() != null) {
                System.out.println("=== USING CACHED AI ANALYSIS AS FALLBACK - Coordinator ===");
                return cached.insight();
            }
            
            return "AI summary is temporarily unavailable due to rate limits. Please try again later.";
        }
    }
}
