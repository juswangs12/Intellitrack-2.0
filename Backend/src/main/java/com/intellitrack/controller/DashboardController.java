package com.intellitrack.controller;

import com.intellitrack.dto.UserDTO;
import com.intellitrack.entity.Deadline;
import com.intellitrack.entity.ProjectGroup;
import com.intellitrack.entity.Submission;
import com.intellitrack.entity.SubmissionStatus;
import com.intellitrack.entity.User;
import com.intellitrack.repository.DeadlineRepository;
import com.intellitrack.repository.DeliverableRepository;
import com.intellitrack.repository.ProjectGroupRepository;
import com.intellitrack.repository.SubmissionRepository;
import com.intellitrack.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/dashboard")
@CrossOrigin(origins = "http://localhost:3000")
@Transactional(readOnly = true)
public class DashboardController {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private DeliverableRepository deliverableRepository;

    @Autowired
    private ProjectGroupRepository projectGroupRepository;

    @Autowired
    private SubmissionRepository submissionRepository;

    @Autowired
    private DeadlineRepository deadlineRepository;

    /**
     * Student dashboard (basic mock data + counts)
     */
    @GetMapping("/student/{id}")
    public ResponseEntity<Map<String, Object>> studentDashboard(@PathVariable Long id) {
        Map<String, Object> resp = new HashMap<>();
        Optional<User> userOptional = userRepository.findById(id);
        ProjectGroup group = userOptional.map(User::getGroup).orElse(null);

        long totalDeliverables = 0;
        long completed = 0;
        long pending = 0;
        long overdue = 0;
        List<Map<String, String>> recent = new ArrayList<>();

        if (group != null) {
            totalDeliverables = deliverableRepository.count();
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
                    .filter(submission -> submission.getStatus() == SubmissionStatus.LATE)
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

        List<User> assigned = userRepository.findByAdvisorId(id);
        List<UserDTO> students = assigned.stream().map(UserDTO::new).collect(Collectors.toList());

        List<ProjectGroup> adviserGroups = projectGroupRepository.findByAdviserId(id);
        List<Submission> allSubmissions = adviserGroups.stream()
                .flatMap(g -> submissionRepository.findByGroupId(g.getId()).stream())
                .collect(Collectors.toList());

        long activeSubmissions = allSubmissions.size();
        long pendingReview = allSubmissions.stream()
                .filter(s -> s.getStatus() == SubmissionStatus.PENDING)
                .count();
        long reviewed = allSubmissions.stream()
                .filter(s -> s.getStatus() == SubmissionStatus.SUBMITTED || s.getStatus() == SubmissionStatus.UPDATED)
                .count();

        resp.put("assignedStudents", students);
        resp.put("assignedCount", students.size());
        resp.put("activeSubmissions", activeSubmissions);
        resp.put("reviewed", reviewed);
        resp.put("pendingReview", pendingReview);

        return ResponseEntity.ok(resp);
    }

    /**
     * Coordinator dashboard: system-level aggregates (mocked)
     */
    @GetMapping("/coordinator/{id}")
    public ResponseEntity<Map<String, Object>> coordinatorDashboard(@PathVariable Long id) {
        Map<String, Object> resp = new HashMap<>();
        long totalStudents = userRepository.findByRole("student").size();
        long totalAdvisers = userRepository.findByRole("adviser").size();
        long submissionsPending = submissionRepository.findByStatus(SubmissionStatus.PENDING).size();

        resp.put("totalStudents", totalStudents);
        resp.put("totalAdvisers", totalAdvisers);
        resp.put("submissionsPending", submissionsPending);
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
        resp.put("createdAt", LocalDateTime.now().toString());
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
