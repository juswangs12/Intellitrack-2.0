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
import com.intellitrack.dto.coordinator.*;
import com.intellitrack.entity.Deliverable;
import com.intellitrack.entity.ProjectGroup;
import com.intellitrack.entity.Subject;
import com.intellitrack.entity.Submission;
import com.intellitrack.entity.SubmissionStatus;
import com.intellitrack.entity.User;
import com.intellitrack.entity.StudentEnrollment;
import com.intellitrack.entity.ClassSection;
import com.intellitrack.entity.ParticipationStatus;
import com.intellitrack.entity.StudentSubmissionParticipation;
import com.intellitrack.repository.DeliverableRepository;
import com.intellitrack.repository.ProjectGroupRepository;
import com.intellitrack.repository.SubmissionRepository;
import com.intellitrack.repository.SubjectRepository;
import com.intellitrack.repository.UserRepository;
import com.intellitrack.repository.StudentSubmissionParticipationRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Objects;
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

    private static final Logger logger = LoggerFactory.getLogger(MetricsCalculationService.class);
    
    private static class SectionStats {
        int totalStudents = 0;
        int onTrack = 0;
        int atRisk = 0;
    }

    private static final String ANALYTICS_SUMMARY_SYSTEM_PROMPT = """
        You are an AI analytics engine for a Capstone Submission Tracking System (IntelliTrack), providing meaningful institutional insights. This is NOT document summarization - you are analyzing academic performance trends.
        
        GENERAL RULES (VERY IMPORTANT):
        - Use FULL student names when appropriate
        - Be factual and evidence-based only
        - Do NOT hallucinate missing data
        - Keep language professional, objective, and academic
        - NO emojis
        - NO fluff
        - NO motivational phrases
        - NO vague statements like "doing well" without specific data
        
        YOUR RESPONSIBILITIES:
        - Identify students at risk
        - Identify groups falling behind
        - Identify sections with poor completion
        - Identify frequently missed deliverables
        - Identify late submission trends
        - Provide overall institutional health metrics
        
        EXAMPLE GOOD OUTPUT (follow this style):
        - Section IT332 currently has the highest late submission rate at 42%.
        - Group Alpha has missed two consecutive deliverables and should be reviewed.
        - Proposal submissions are healthy with a 94% completion rate, while Chapter 2 has only reached 61%.
        - 18 students remain ungrouped.
        - Overall institutional completion currently stands at 73%.
        
        EXAMPLE BAD OUTPUT (avoid this):
        - "Everything looks good."
        - "Students should work harder."
        - "Keep up the good work."
        
        RESPONSE STYLE:
        - Provide concise, bullet-point style insights
        - Each bullet should be a complete, meaningful sentence
        - Focus on trends, risks, and actionable observations
        - Only analyze the provided data

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
    private final com.intellitrack.repository.StudentEnrollmentRepository studentEnrollmentRepository;

    public MetricsCalculationService(
            SubmissionRepository submissionRepository,
            DeliverableRepository deliverableRepository,
            ProjectGroupRepository projectGroupRepository,
            SubjectRepository subjectRepository,
            UserRepository userRepository,
            StatusEvaluationService statusEvaluationService,
            AiService aiService,
            StudentSubmissionParticipationRepository participationRepository,
            com.intellitrack.repository.StudentEnrollmentRepository studentEnrollmentRepository) {
        this.submissionRepository = submissionRepository;
        this.deliverableRepository = deliverableRepository;
        this.projectGroupRepository = projectGroupRepository;
        this.subjectRepository = subjectRepository;
        this.userRepository = userRepository;
        this.statusEvaluationService = statusEvaluationService;
        this.aiService = aiService;
        this.participationRepository = participationRepository;
        this.studentEnrollmentRepository = studentEnrollmentRepository;
    }

    public CoordinatorDashboardDto buildCoordinatorDashboard() {
        List<ProjectGroup> allGroups = projectGroupRepository.findAll();
        List<StudentEnrollment> allEnrollments = studentEnrollmentRepository.findAllWithSectionAndSubject();
        List<Deliverable> allDeliverables = deliverableRepository.findAll();
        // Fetch only submissions for students present in enrollments and the deliverables list
        List<Long> studentIds = allEnrollments.stream()
            .map(StudentEnrollment::getStudent)
            .filter(Objects::nonNull)
            .map(User::getId)
            .distinct()
            .collect(Collectors.toList());
        List<Long> deliverableIds = allDeliverables.stream().map(Deliverable::getId).collect(Collectors.toList());

        List<Submission> allSubmissions = studentIds.isEmpty() || deliverableIds.isEmpty()
            ? List.of()
            : submissionRepository.findByStudentIdInAndDeliverableIdIn(studentIds, deliverableIds);
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
            logger.debug("Returning cached AI analysis for coordinator summary");
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
            logger.info("Generating new AI analysis for coordinator summary");
            
            CoordinatorAnalyticsV2Dto analyticsV2 = null;
            StringBuilder promptBuilder = new StringBuilder();
            
            try {
                analyticsV2 = buildCoordinatorAnalyticsV2();
                
                promptBuilder.append("COORDINATOR ANALYTICS DATA:\n\n");
                
                // Overall institutional metrics
                promptBuilder.append("=== OVERALL INSTITUTIONAL METRICS ===\n");
                long totalStudents = students.size();
                long totalGroups = subjects.stream().mapToLong(SubjectAnalyticsDto::getTotalGroups).sum();
                double avgCompliance = subjects.stream().mapToDouble(SubjectAnalyticsDto::getComplianceRate).average().orElse(0.0);
                promptBuilder.append(String.format("- Total Students: %d\n", totalStudents));
                promptBuilder.append(String.format("- Total Groups: %d\n", totalGroups));
                promptBuilder.append(String.format("- Average Compliance Rate: %.0f%%\n", avgCompliance));
                
                // Ungrouped students count
                long ungroupedStudents = students.stream()
                        .filter(s -> s.groupTitle() == null || "Not Assigned".equals(s.groupTitle()))
                        .count();
                promptBuilder.append(String.format("- Ungrouped Students: %d\n", ungroupedStudents));
                
                promptBuilder.append("\n=== SUBJECT AND GROUP BREAKDOWN ===\n");
                for (SubjectAnalyticsDto subject : subjects) {
                    promptBuilder.append(String.format("\n--- SUBJECT: %s (%s) ---\n",
                            subject.getSubjectName(), subject.getSubjectCode()));
                    promptBuilder.append(String.format("- Total Groups: %d\n", subject.getTotalGroups()));
                    promptBuilder.append(String.format("- Total Students: %d\n", subject.getTotalStudents()));
                    promptBuilder.append(String.format("- Compliance Rate: %.0f%%\n", subject.getComplianceRate()));
                    promptBuilder.append(String.format("- Late Rate: %.0f%%\n", subject.getLateSubmissionRate()));
                    promptBuilder.append(String.format("- Missing Rate: %.0f%%\n", subject.getMissingSubmissionRate()));
                    
                    promptBuilder.append("\n  GROUPS:\n");
                    for (var group : subject.getGroupCompliance()) {
                        promptBuilder.append(String.format("\n  Group: %s (%s)\n", group.getGroupTitle(), group.getGroupCode()));
                        promptBuilder.append(String.format("    - Compliance: %.0f%%\n", group.getComplianceRate()));
                        promptBuilder.append(String.format("    - On-Time Deliverables: %d\n", group.getSubmittedDeliverables() - group.getLateDeliverables()));
                        promptBuilder.append(String.format("    - Late Deliverables: %d\n", group.getLateDeliverables()));
                        promptBuilder.append(String.format("    - Missing Deliverables: %d\n", group.getMissingDeliverables()));
                    }
                    
                    if (subject.getRiskGroups() != null && subject.getRiskGroups().size() > 0) {
                        promptBuilder.append("\n  AT-RISK GROUPS:\n");
                        for (var riskGroup : subject.getRiskGroups()) {
                            promptBuilder.append(String.format("    - %s: Risk Level %s - %s\n",
                                    riskGroup.groupTitle(), riskGroup.riskLevel(), riskGroup.reason()));
                        }
                    }
                }
                
                // Section statistics from analyticsV2
            if (analyticsV2 != null && analyticsV2.sections() != null && !analyticsV2.sections().isEmpty()) {
                promptBuilder.append("\n=== SECTION STATISTICS ===\n");
                // Let's calculate section-level metrics from student rows
                Map<String, SectionStats> sectionStatsMap = new HashMap<>();
                for (var studentRow : analyticsV2.students()) {
                    String sectionKey = studentRow.subject() + " - " + studentRow.section();
                    SectionStats stats = sectionStatsMap.computeIfAbsent(sectionKey, k -> new SectionStats());
                    stats.totalStudents++;
                    if (studentRow.completionRate() != null && studentRow.completionRate() >= 80) stats.onTrack++;
                    if (studentRow.completionRate() != null && studentRow.completionRate() < 50) stats.atRisk++;
                }
                    
                    for (var entry : sectionStatsMap.entrySet()) {
                        promptBuilder.append(String.format("\n- %s:\n", entry.getKey()));
                        promptBuilder.append(String.format("  Total Students: %d\n", entry.getValue().totalStudents));
                        promptBuilder.append(String.format("  On Track (>=80%%): %d\n", entry.getValue().onTrack));
                        promptBuilder.append(String.format("  At Risk (<50%%): %d\n", entry.getValue().atRisk));
                    }
                }
                
                promptBuilder.append("\n\n=== STUDENT DETAILS ===\n");
                for (StudentAnalyticsDto student : students) {
                    promptBuilder.append(String.format("- %s (Group: %s, Subject: %s): On-time: %d, Late: %d, Missing: %d, Compliance: %.0f%%\n",
                            student.studentName(), student.groupTitle(), student.subjectCode(),
                            student.onTimeDeliverables(), student.lateDeliverables(), student.missingDeliverables(),
                            student.complianceRate()));
                }
                
            } catch (Exception e) {
                // Fallback to simpler prompt if analyticsV2 fails
                logger.warn("Error building detailed prompt, using fallback: {}", e.getMessage());
                List<SubjectAnalyticsDto> fallbackSubjects = buildSubjectAnalytics();
                List<StudentAnalyticsDto> fallbackStudents = buildStudentAnalytics();
                
                promptBuilder = new StringBuilder();
                promptBuilder.append("COORDINATOR ANALYTICS DATA:\n\n");
                promptBuilder.append("SUBJECT AND GROUP BREAKDOWN:\n");
                for (SubjectAnalyticsDto subject : fallbackSubjects) {
                    promptBuilder.append(String.format("\n=== SUBJECT: %s (%s) ===\n",
                            subject.getSubjectName(), subject.getSubjectCode()));
                    promptBuilder.append(String.format("- Total Groups: %d\n", subject.getTotalGroups()));
                    promptBuilder.append(String.format("- Total Students: %d\n", subject.getTotalStudents()));
                    promptBuilder.append(String.format("- Compliance Rate: %.0f%%\n", subject.getComplianceRate()));
                }
            }
            
            String summary = aiService.getCompletion(promptBuilder.toString(), ANALYTICS_SUMMARY_SYSTEM_PROMPT).block();
            
            if (summary != null && !summary.trim().isEmpty()) {
                coordinatorInsightCache = new CachedInsight(summary, Instant.now(), Instant.now());
                logger.info("AI analysis generated and cached for coordinator");
                return summary;
            } else {
                if (cached != null && cached.insight() != null) {
                    logger.debug("AI returned empty response, using cached analysis as fallback");
                    return cached.insight();
                }
                return "AI summary is not available. Please configure your AI_API_KEY environment variable to enable AI features.";
            }
            
        } catch (Exception e) {
            logger.warn("AI analysis failed (rate limit or error): {}", e.getMessage());
            
            if (cached != null && cached.insight() != null) {
                logger.debug("Using cached AI analysis as fallback");
                return cached.insight();
            }
            
            return "AI summary is temporarily unavailable due to rate limits. Please try again later.";
        }
    }
    
    @Transactional(readOnly = true)
    public CoordinatorAnalyticsV2Dto buildCoordinatorAnalyticsV2() {
        // Step 1: Fetch all base data
        // NOTE: Use findAllWithSectionAndSubject() for enrollments (section/subject eager-loaded).
        // Group and adviser data is resolved via a reverse map from findAllWithAllRelations()
        // on ProjectGroupRepository — eliminating the N+1 on enrollment.getGroups().
        List<StudentEnrollment> allEnrollments = studentEnrollmentRepository.findAllWithSectionAndSubject();
        List<Deliverable> allDeliverables = deliverableRepository.findAll();
        List<Subject> allSubjects = subjectRepository.findAll();
        // Use findAllWithAllRelations to eagerly load adviser + students (enrollments) in one query.
        // This replaces projectGroupRepository.findAll() and eliminates the N+1 from
        // enrollment.getGroups() calls inside the student-row building loop below.
        List<ProjectGroup> allGroups = projectGroupRepository.findAllWithAllRelations();
        List<User> allAdvisers = userRepository.findByRole("adviser");
        List<Submission> allSubmissions = submissionRepository.findAll();

        // Step 2: Create lookup maps for efficiency
        Map<Long, Deliverable> deliverableById = allDeliverables.stream()
                .collect(Collectors.toMap(Deliverable::getId, d -> d));
        Map<String, Submission> submissionByStudentAndDeliverable = new HashMap<>();
        for (Submission sub : allSubmissions) {
            if (sub.getStudent() != null && sub.getDeliverable() != null) {
                String key = sub.getStudent().getId() + "_" + sub.getDeliverable().getId();
                submissionByStudentAndDeliverable.put(key, sub);
            }
        }
        // Build reverse lookup: enrollmentId → ProjectGroup (avoids lazy-loading enrollment.getGroups())
        Map<Long, ProjectGroup> groupByEnrollmentId = new HashMap<>();
        for (ProjectGroup g : allGroups) {
            if (g.getStudents() != null) {
                for (StudentEnrollment se : g.getStudents()) {
                    groupByEnrollmentId.put(se.getId(), g);
                }
            }
        }

        // Step 3: Build meta DTOs
        List<DeliverableMetaDto> deliverableMetas = allDeliverables.stream()
                .map(d -> new DeliverableMetaDto(
                        d.getId(),
                        d.getName(),
                        d.getStage(),
                        d.getDueAt(),
                        d.getSubject() != null ? d.getSubject().getId() : null
                ))
                .collect(Collectors.toList());

        List<SubjectMetaDto> subjectMetas = allSubjects.stream()
                .map(s -> new SubjectMetaDto(s.getId(), s.getCode(), s.getName()))
                .collect(Collectors.toList());

        List<AdviserMetaDto> adviserMetas = allAdvisers.stream()
                .map(a -> new AdviserMetaDto(a.getId(), a.getFirstName() + " " + a.getLastName()))
                .collect(Collectors.toList());

        // Collect unique sections
        Set<ClassSection> uniqueSections = allEnrollments.stream()
                .map(StudentEnrollment::getClassSection)
                .filter(cs -> cs != null)
                .collect(Collectors.toSet());
        List<SectionMetaDto> sectionMetas = uniqueSections.stream()
                .map(cs -> new SectionMetaDto(
                        cs.getId(),
                        cs.getSection(),
                        cs.getSubject() != null ? cs.getSubject().getId() : null
                ))
                .collect(Collectors.toList());

        List<GroupMetaDto> groupMetas = allGroups.stream()
                .map(g -> new GroupMetaDto(
                        g.getId(),
                        g.getCode(),
                        g.getTitle(),
                        g.getAdviser() != null ? g.getAdviser().getId() : null,
                        g.getSubject() != null ? g.getSubject().getId() : null
                ))
                .collect(Collectors.toList());

        // Step4: Build student rows
        List<StudentAnalyticsRowDto> studentRows = new ArrayList<>();
        LocalDateTime now = LocalDateTime.now();

        long totalStudents = 0;
        long studentsSubmitted = 0;
        long studentsLate = 0;
        long studentsMissing = 0;
        double totalCompletion = 0.0;
        double totalSubmission = 0.0;
        long pendingCount = 0;

        for (StudentEnrollment enrollment : allEnrollments) {
            totalStudents++;
            // Determine account status
            String accountStatus;
            Long userId = null;
            if (enrollment.getStudent() != null) {
                accountStatus = "LINKED";
                userId = enrollment.getStudent().getId();
            } else {
                // Check if user exists but isn't linked (based on email?)
                // For now, we'll mark as NOT_LINKED
                accountStatus = "NOT_LINKED";
            }

            // Get student's group — use reverse lookup map (no lazy SQL per enrollment)
            ProjectGroup studentGroup = groupByEnrollmentId.get(enrollment.getId());
            String groupName = studentGroup != null ? studentGroup.getTitle() : "Not Assigned";
            String adviserName = (studentGroup != null && studentGroup.getAdviser() != null)
                    ? studentGroup.getAdviser().getFirstName() + " " + studentGroup.getAdviser().getLastName()
                    : null;

            // Get subject/section
            String subject = enrollment.getClassSection() != null && enrollment.getClassSection().getSubject() != null
                    ? enrollment.getClassSection().getSubject().getCode()
                    : "N/A";
            String section = enrollment.getClassSection() != null
                    ? enrollment.getClassSection().getSection()
                    : "N/A";

            // Build deliverable cells for this student
            List<StudentDeliverableCellDto> cells = new ArrayList<>();
            long onTimeCount = 0;
            long lateCount = 0;
            long missingCount = 0;
            boolean hasSubmitted = false;
            boolean isLate = false;

            for (Deliverable deliverable : allDeliverables) {
                String cellStatus;
                Integer lateDays = null;
                LocalDateTime submittedAt = null;
                String displayText = "Not Yet Assigned";
                String aiStatus = null;

                // Only consider deliverables for the student's subject, or all if no subject
                boolean isRelevant = true;
                if (enrollment.getClassSection() != null && enrollment.getClassSection().getSubject() != null) {
                    Long studentSubjectId = enrollment.getClassSection().getSubject().getId();
                    Long deliverableSubjectId = deliverable.getSubject() != null ? deliverable.getSubject().getId() : null;
                    if (deliverableSubjectId != null && !deliverableSubjectId.equals(studentSubjectId)) {
                        // Skip deliverables from other subjects
                        isRelevant = false;
                    }
                }

                if (!isRelevant) {
                    cellStatus = "NOT_YET_ASSIGNED";
                    displayText = "N/A";
                } else {
                    // Check for submission
                    Submission submission = null;
                    if (userId != null) {
                        String key = userId + "_" + deliverable.getId();
                        submission = submissionByStudentAndDeliverable.get(key);
                    }

                    if (submission != null && submission.getSubmittedAt() != null) {
                        submittedAt = submission.getSubmittedAt();
                        hasSubmitted = true;
                        // Capture AI analysis status from submission
                        aiStatus = submission.getAiStatus() != null ? submission.getAiStatus().name() : null;
                        // Compare to deadline
                        if (deliverable.getDueAt() != null) {
                            if (submittedAt.isBefore(deliverable.getDueAt()) || submittedAt.isEqual(deliverable.getDueAt())) {
                                cellStatus = "SUBMITTED_ON_TIME";
                                displayText = "Submitted ✔";
                                onTimeCount++;
                            } else {
                                cellStatus = "SUBMITTED_LATE";
                                long daysLate = Duration.between(deliverable.getDueAt(), submittedAt).toDays();
                                lateDays = (int) daysLate;
                                displayText = "Late " + daysLate + " days";
                                lateCount++;
                                isLate = true;
                            }
                        } else {
                            cellStatus = "SUBMITTED_ON_TIME";
                            displayText = "Submitted ✔";
                            onTimeCount++;
                        }
                    } else {
                        // No submission
                        if (deliverable.getDueAt() != null) {
                            if (now.isAfter(deliverable.getDueAt())) {
                                cellStatus = "DID_NOT_SUBMIT";
                                displayText = "Did Not Submit";
                                missingCount++;
                            } else if (now.toLocalDate().equals(deliverable.getDueAt().toLocalDate())) {
                                cellStatus = "DUE_TODAY";
                                displayText = "Due Today";
                                pendingCount++;
                            } else {
                                long daysUntilDue = Duration.between(now, deliverable.getDueAt()).toDays();
                                cellStatus = "UPCOMING";
                                displayText = "Upcoming (" + daysUntilDue + " days)";
                                pendingCount++;
                            }
                        } else {
                            cellStatus = "NOT_YET_ASSIGNED";
                            displayText = "Not Yet Assigned";
                        }
                    }
                }

                cells.add(new StudentDeliverableCellDto(
                        deliverable.getId(),
                        cellStatus,
                        lateDays,
                        submittedAt,
                        displayText,
                        aiStatus
                ));
            }

            if (hasSubmitted) studentsSubmitted++;
            if (isLate) studentsLate++;
            if (missingCount > 0) studentsMissing++;

            long totalRelevantDeliverables = cells.stream()
                    .filter(c -> !c.status().equals("NOT_YET_ASSIGNED"))
                    .count();
            double completionRate = totalRelevantDeliverables > 0
                    ? roundToOneDecimal(((double) onTimeCount / totalRelevantDeliverables) * 100.0)
                    : 0.0;
            double submissionRate = totalRelevantDeliverables > 0
                    ? roundToOneDecimal(((double) (onTimeCount + lateCount) / totalRelevantDeliverables) * 100.0)
                    : 0.0;
            totalCompletion += completionRate;
            totalSubmission += submissionRate;

            studentRows.add(new StudentAnalyticsRowDto(
                    enrollment.getId(),
                    enrollment.getFullName(),
                    enrollment.getStudentId(),
                    section,
                    subject,
                    groupName,
                    adviserName,
                    accountStatus,
                    userId,
                    cells,
                    completionRate,
                    onTimeCount,
                    lateCount,
                    missingCount
            ));
        }

        // Build summary cards
        double overallCompletionRate = totalStudents > 0 ? roundToOneDecimal(totalCompletion / totalStudents) : 0.0;
        double overallSubmissionRate = totalStudents > 0 ? roundToOneDecimal(totalSubmission / totalStudents) : 0.0;

        long totalDeliverables = allDeliverables.size();
        double institutionRiskScore = totalStudents > 0 ? roundToOneDecimal((double) studentsMissing * 100.0 / totalStudents) : 0.0;

        SummaryCardsDto summaryCards = new SummaryCardsDto(
            totalStudents,
            (long) allGroups.size(),
            (long) allAdvisers.size(),
            studentsSubmitted,
            studentsLate,
            studentsMissing,
            overallCompletionRate,
            overallSubmissionRate,
            totalDeliverables,
            pendingCount,
            institutionRiskScore
        );

        return new CoordinatorAnalyticsV2Dto(
                summaryCards,
                deliverableMetas,
                studentRows,
                subjectMetas,
                sectionMetas,
                groupMetas,
                adviserMetas
        );
    }
}
