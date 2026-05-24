package com.intellitrack.controller;

import com.intellitrack.dto.UserDTO;
import com.intellitrack.entity.Deadline;
import com.intellitrack.entity.ProjectGroup;
import com.intellitrack.entity.StudentEnrollment;
import com.intellitrack.entity.Submission;
import com.intellitrack.entity.SubmissionStatus;
import com.intellitrack.entity.User;
import com.intellitrack.repository.DeadlineRepository;
import com.intellitrack.repository.DeliverableRepository;
import com.intellitrack.repository.ProjectGroupRepository;
import com.intellitrack.repository.StudentEnrollmentRepository;
import com.intellitrack.repository.SubmissionRepository;
import com.intellitrack.repository.UserRepository;
import com.intellitrack.service.AiService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/dashboard")
@Transactional(readOnly = true)
@SuppressWarnings({ "deprecation", "null" })
public class DashboardController {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private DeliverableRepository deliverableRepository;

    @Autowired
    private SubmissionRepository submissionRepository;

    @Autowired
    private DeadlineRepository deadlineRepository;

    @Autowired
    private ProjectGroupRepository projectGroupRepository;

    @Autowired
    private StudentEnrollmentRepository studentEnrollmentRepository;

    @Autowired
    private AiService aiService;

    /**
     * Student dashboard (basic mock data + counts)
     */
    @GetMapping("/student/{id}")
    public ResponseEntity<Map<String, Object>> studentDashboard(@PathVariable Long id) {
        Map<String, Object> resp = new HashMap<>();
        Optional<User> userOptional = userRepository.findById(id);
        ProjectGroup group = null;
        if (userOptional.isPresent()) {
            User user = userOptional.get();
            // First try to get group from StudentEnrollment (new architecture)
            List<StudentEnrollment> enrollments = studentEnrollmentRepository.findByStudent_Id(user.getId());
            if (!enrollments.isEmpty()) {
                StudentEnrollment enrollment = enrollments.get(0);
                if (!enrollment.getGroups().isEmpty()) {
                    group = enrollment.getGroups().get(0);
                }
            }
            // Fall back to old user.group field for backward compatibility
            if (group == null) {
                group = user.getGroup();
            }
        }

        long totalDeliverables = deliverableRepository.count();
        long completed = 0;
        long pending = totalDeliverables;
        long overdue = 0;
        List<Map<String, String>> recent = new ArrayList<>();

        if (group != null) {
            List<Submission> submissions = submissionRepository.findByGroupId(group.getId());
            completed = submissions.stream()
                    .filter(submission -> submission.getSubmittedAt() != null)
                    .filter(submission -> {
                        Deadline deadline = deadlineRepository.findByDeliverableId(submission.getDeliverable().getId())
                                .orElse(null);
                        return deadline == null || !submission.getSubmittedAt().isAfter(deadline.getDueAt());
                    })
                    .count();

            overdue = submissions.stream()
                    .filter(submission -> submission.getStatus() == SubmissionStatus.SUBMITTED_LATE)
                    .count();

            pending = Math.max(0, totalDeliverables - completed - overdue);

            submissions.stream()
                    .sorted(Comparator.comparing(
                            submission -> Optional.ofNullable(submission.getSubmittedAt()).orElse(LocalDateTime.MIN),
                            Comparator.reverseOrder()))
                    .limit(3)
                    .forEach(submission -> recent.add(Map.of(
                            "text",
                            submission.getDeliverable().getName() + " is currently " + submission.getStatus().name(),
                            "time",
                            formatTime(submission.getSubmittedAt(),
                                    deadlineRepository.findByDeliverableId(submission.getDeliverable().getId())
                                            .map(Deadline::getDueAt).orElse(null)))));

            resp.put("aiInsight", "AI insights are available via dedicated AI endpoints.");
        }

        if (recent.isEmpty()) {
            recent.add(Map.of("text", "No submission activity yet", "time", "Waiting for first update"));
        }

        resp.put("groupTitle", group == null ? null : group.getTitle());
        resp.put("totalDeliverables", totalDeliverables);
        resp.put("completed", completed);
        resp.put("pending", pending);
        resp.put("overdue", overdue);

        resp.put("recentActivity", recent);
        return ResponseEntity.ok(resp);
    }

    /**
     * Adviser dashboard: return assigned students and simple stats
     */
    @GetMapping("/adviser/{id}")
    public ResponseEntity<Map<String, Object>> adviserDashboard(@PathVariable Long id) {
        Map<String, Object> resp = new HashMap<>();

        List<ProjectGroup> assignedGroups = projectGroupRepository.findByAdviserId(id);
        List<User> assignedStudents = new ArrayList<>();

        long totalSubmissions = 0;
        long onTime = 0;
        long late = 0;
        long notSubmitted = 0;

        for (ProjectGroup group : assignedGroups) {
            for (com.intellitrack.entity.StudentEnrollment enrollment : group.getStudents()) {
                if (enrollment.getStudent() != null) {
                    assignedStudents.add(enrollment.getStudent());
                }
            }
            List<Submission> submissions = submissionRepository.findByGroupId(group.getId());
            totalSubmissions += submissions.size();
            for (Submission sub : submissions) {
                SubmissionStatus normalized = sub.getStatus() == null ? SubmissionStatus.PENDING
                        : sub.getStatus().normalize();
                if (normalized == SubmissionStatus.SUBMITTED_ON_TIME)
                    onTime++;
                else if (normalized == SubmissionStatus.SUBMITTED_LATE)
                    late++;
                else if (normalized == SubmissionStatus.NOT_SUBMITTED)
                    notSubmitted++;
            }
        }

        List<UserDTO> studentDtos = assignedStudents.stream().map(UserDTO::new).collect(Collectors.toList());

        resp.put("assignedStudents", studentDtos);
        resp.put("assignedCount", studentDtos.size());
        resp.put("assignedGroupsCount", assignedGroups.size());
        resp.put("totalSubmissions", totalSubmissions);
        resp.put("onTime", onTime);
        resp.put("late", late);
        resp.put("notSubmitted", notSubmitted);

        // Generate AI insight for the adviser based on their group stats
        try {
            String prompt = String.format(
                "You are InsightHub AI for IntelliTrack. Write 3 short bullet points for an adviser's dashboard. " +
                "Facts: assignedGroups=%d, assignedStudents=%d, totalSubmissions=%d, onTime=%d, late=%d, notSubmitted=%d. " +
                "Each bullet should be 10-16 words, actionable, and professional. " +
                "Format exactly as:\n- ...\n- ...\n- ...",
                assignedGroups.size(), studentDtos.size(), totalSubmissions, onTime, late, notSubmitted);
            String systemPrompt = "You are InsightHub AI for IntelliTrack. " +
                "Respond with plain text bullet points only. " +
                "No JSON, no markdown headers, no code blocks.";
            String aiInsight = aiService.getCompletion(prompt, systemPrompt).block();
            if (aiInsight != null && !aiInsight.isBlank()) {
                resp.put("aiInsight", aiInsight);
            }
        } catch (Exception ignored) {
            // AI unavailable — omit the field so the frontend panel stays hidden
        }

        return ResponseEntity.ok(resp);
    }

    /**
     * Coordinator dashboard: system-level aggregates
     */
    @GetMapping("/coordinator/{id}")
    public ResponseEntity<Map<String, Object>> coordinatorDashboard(@PathVariable Long id) {
        Map<String, Object> resp = new HashMap<>();
        long totalStudents = studentEnrollmentRepository.count();
        long totalAdvisers = userRepository.findByRole("adviser").size();

        // Include legacy SUBMITTED rows in the on-time count
        long submissionsOnTime = submissionRepository.findByStatus(SubmissionStatus.SUBMITTED_ON_TIME).size()
                + submissionRepository.findByStatus(SubmissionStatus.SUBMITTED).size();
        long submissionsLate = submissionRepository.findByStatus(SubmissionStatus.SUBMITTED_LATE).size();
        long submissionsNotSubmitted = submissionRepository.findByStatus(SubmissionStatus.NOT_SUBMITTED).size();
        long totalGroups = projectGroupRepository.count();

        resp.put("totalStudents", totalStudents);
        resp.put("totalAdvisers", totalAdvisers);
        resp.put("submissionsOnTime", submissionsOnTime);
        resp.put("submissionsLate", submissionsLate);
        resp.put("submissionsNotSubmitted", submissionsNotSubmitted);
        resp.put("totalGroups", totalGroups);

        resp.put("recentNotifications", List.of());
        return ResponseEntity.ok(resp);
    }

    /**
     * Admin dashboard: counts and system stats
     */
    @GetMapping("/admin/{id}")
    public ResponseEntity<Map<String, Object>> adminDashboard(@PathVariable Long id) {
        Map<String, Object> resp = new HashMap<>();
        long totalUsers = userRepository.count();
        long students = userRepository.findByRole("student").size();
        long advisers = userRepository.findByRole("adviser").size();
        long coordinators = userRepository.findByRole("coordinator").size();
        long admins = userRepository.findByRole("administrator").size();

        resp.put("totalUsers", totalUsers);
        resp.put("byRole", Map.of(
                "students", students,
                "advisers", advisers,
                "coordinators", coordinators,
                "administrators", admins));
        resp.put("timestamp", LocalDateTime.now());
        return ResponseEntity.ok(resp);
    }

    private String formatTime(LocalDateTime submittedAt, LocalDateTime dueAt) {
        if (submittedAt != null) {
            return submittedAt.toLocalDate().toString();
        }

        if (dueAt != null) {
            long daysRemaining = java.time.Duration.between(LocalDateTime.now(), dueAt).toDays();
            return daysRemaining >= 0 ? daysRemaining + " days left" : Math.abs(daysRemaining) + " days overdue";
        }

        return "No timeline available";
    }
}
