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
@CrossOrigin(origins = "http://localhost:3000")
@Transactional(readOnly = true)
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
    private AiService aiService;

    @Autowired
    private ProjectGroupRepository projectGroupRepository;

    @Autowired
    private StudentEnrollmentRepository studentEnrollmentRepository;

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
            
            // AI Insight for student
            String aiPrompt = String.format("Analyze student progress: %d completed, %d pending, %d overdue. Provide a 1-sentence tip.", completed, pending, overdue);
            String aiInsight = aiService.getCompletion(aiPrompt).block();
            resp.put("aiInsight", aiInsight);
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
        
        long pendingReview = 0;
        long onTrack = 0;
        long overdue = 0;

        for (ProjectGroup group : assignedGroups) {
            for (com.intellitrack.entity.StudentEnrollment enrollment : group.getStudents()) {
                if (enrollment.getStudent() != null) {
                    assignedStudents.add(enrollment.getStudent());
                }
            }
            List<Submission> submissions = submissionRepository.findByGroupId(group.getId());
            for (Submission sub : submissions) {
                if (sub.getStatus() == SubmissionStatus.SUBMITTED || sub.getStatus() == SubmissionStatus.UPDATED) pendingReview++;
                else if (sub.getStatus() == SubmissionStatus.APPROVED) onTrack++;
                else if (sub.getStatus() == SubmissionStatus.LATE) overdue++;
            }
        }

        List<UserDTO> studentDtos = assignedStudents.stream().map(UserDTO::new).collect(Collectors.toList());

        resp.put("assignedStudents", studentDtos);
        resp.put("assignedCount", studentDtos.size());
        resp.put("assignedGroupsCount", assignedGroups.size());
        resp.put("pendingReview", pendingReview);
        resp.put("onTrack", onTrack);
        resp.put("overdue", overdue);

        // AI Insight for adviser
        String aiPrompt = String.format("Adviser dashboard summary: %d groups and %d students assigned. %d pending reviews, %d approved, %d overdue. Provide a 1-sentence professional summary.", 
            assignedGroups.size(), studentDtos.size(), pendingReview, onTrack, overdue);
        String aiInsight = aiService.getCompletion(aiPrompt).block();
        resp.put("aiInsight", aiInsight);

        return ResponseEntity.ok(resp);
    }

    /**
     * Coordinator dashboard: system-level aggregates
     */
    @GetMapping("/coordinator/{id}")
    public ResponseEntity<Map<String, Object>> coordinatorDashboard(@PathVariable Long id) {
        Map<String, Object> resp = new HashMap<>();
        long totalStudents = userRepository.findByRole("student").size();
        long totalAdvisers = userRepository.findByRole("adviser").size();
        
        // Count submissions waiting for review
        long submissionsPending = submissionRepository.findByStatusIn(
            List.of(SubmissionStatus.SUBMITTED, SubmissionStatus.UPDATED)
        ).size();
        
        long submissionsLate = submissionRepository.findByStatus(SubmissionStatus.LATE).size();

        resp.put("totalStudents", totalStudents);
        resp.put("totalAdvisers", totalAdvisers);
        resp.put("submissionsPending", submissionsPending);
        resp.put("submissionsLate", submissionsLate);
        
        // AI Insight for coordinator
        String aiPrompt = String.format("Coordinator system summary: %d students, %d advisers. %d submissions waiting for review, %d late. Provide a 1-sentence strategic overview.", totalStudents, totalAdvisers, submissionsPending, submissionsLate);
        String aiInsight = aiService.getCompletion(aiPrompt).block();
        resp.put("aiInsight", aiInsight);

        resp.put("recentNotifications", List.of()); // Rely on real notifications API
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
