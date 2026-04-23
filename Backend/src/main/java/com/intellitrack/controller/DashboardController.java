package com.intellitrack.controller;

import com.intellitrack.dto.UserDTO;
import com.intellitrack.entity.User;
import com.intellitrack.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/dashboard")
@CrossOrigin(origins = "http://localhost:3000")
public class DashboardController {

    @Autowired
    private UserRepository userRepository;

    /**
     * Student dashboard (basic mock data + counts)
     */
    @GetMapping("/student/{id}")
    public ResponseEntity<Map<String, Object>> studentDashboard(@PathVariable Long id) {
        Map<String, Object> resp = new HashMap<>();
        // Basic counts - mocked because submissions not implemented yet
        resp.put("totalDeliverables", 5);
        resp.put("completed", 3);
        resp.put("pending", 2);
        resp.put("overdue", 0);

        List<Map<String, String>> recent = new ArrayList<>();
        recent.add(Map.of("text", "Submitted Project Proposal", "time", "2 hours ago"));
        recent.add(Map.of("text", "Deadline approaching: Final Report", "time", "3 days left"));
        recent.add(Map.of("text", "New feedback from Adviser", "time", "1 day ago"));

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

        resp.put("assignedStudents", students);
        resp.put("assignedCount", students.size());
        resp.put("activeSubmissions", 24);
        resp.put("reviewed", 16);
        resp.put("pendingReview", 8);

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
        resp.put("totalStudents", totalStudents);
        resp.put("totalAdvisers", totalAdvisers);
        resp.put("submissionsPending", 42);
        resp.put("recentNotifications", List.of("Submission deadline next week", "New adviser assigned"));
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
                "administrators", admins
        ));
        resp.put("createdAt", LocalDateTime.now().toString());
        return ResponseEntity.ok(resp);
    }
}
