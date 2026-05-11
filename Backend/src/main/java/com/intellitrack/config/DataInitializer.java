package com.intellitrack.config;

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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
public class DataInitializer {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ProjectGroupRepository projectGroupRepository;

    @Autowired
    private DeliverableRepository deliverableRepository;

    @Autowired
    private DeadlineRepository deadlineRepository;

    @Autowired
    private SubmissionRepository submissionRepository;

    @EventListener(ApplicationReadyEvent.class)
    public void runAfterStartup(ApplicationReadyEvent event) {
        try {
            // Only initialize the system administrator if no users exist
            User admin, coordinator, adviser, student;
            if (userRepository.count() == 0) {
                BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

                // Create initial System Administrator
                admin = new User();
                admin.setFirstName("System");
                admin.setLastName("Administrator");
                admin.setEmail("admin@intellitrack.com");
                admin.setPassword(passwordEncoder.encode("IntelliTrack@2026!"));
                admin.setRole("administrator");
                admin.setDepartment("Information Technology");
                admin.setPhone("+63000000000");
                admin.setCreatedAt(LocalDateTime.now());
                userRepository.save(admin);

                // Create initial Coordinator
                coordinator = new User();
                coordinator.setFirstName("Academic");
                coordinator.setLastName("Coordinator");
                coordinator.setEmail("coordinator@intellitrack.com");
                coordinator.setPassword(passwordEncoder.encode("Coord@2026!"));
                coordinator.setRole("coordinator");
                coordinator.setDepartment("Information Technology");
                coordinator.setPhone("+63111111111");
                coordinator.setCreatedAt(LocalDateTime.now());
                userRepository.save(coordinator);

                // Create initial Adviser
                adviser = new User();
                adviser.setFirstName("Technical");
                adviser.setLastName("Adviser");
                adviser.setEmail("adviser@intellitrack.com");
                adviser.setPassword(passwordEncoder.encode("Adviser@2026!"));
                adviser.setRole("adviser");
                adviser.setDepartment("Information Technology");
                adviser.setPhone("+63222222222");
                adviser.setCreatedAt(LocalDateTime.now());
                userRepository.save(adviser);

                // Create initial Student
                student = new User();
                student.setFirstName("Sample");
                student.setLastName("Student");
                student.setEmail("student@intellitrack.com");
                student.setPassword(passwordEncoder.encode("Student@2026!"));
                student.setRole("student");
                student.setDepartment("Information Technology");
                student.setPhone("+63333333333");
                student.setCreatedAt(LocalDateTime.now());
                userRepository.save(student);

                System.out.println("System initialized: Admin, Coordinator, and Adviser accounts created.");
            } else {
                // Fetch existing users for sample data
                admin = userRepository.findByEmail("admin@intellitrack.com").orElse(null);
                coordinator = userRepository.findByEmail("coordinator@intellitrack.com").orElse(null);
                adviser = userRepository.findByEmail("adviser@intellitrack.com").orElse(null);
                student = userRepository.findByEmail("student@intellitrack.com").orElse(null);
            }

            // Initialize some sample deliverables if none exist
            if (deliverableRepository.count() == 0) {
                Deliverable proposal = new Deliverable();
                proposal.setName("Project Proposal");
                proposal.setStage("Proposal");
                deliverableRepository.save(proposal);

                Deliverable srs = new Deliverable();
                srs.setName("SRS Document");
                srs.setStage("Midterm");
                deliverableRepository.save(srs);

                Deliverable sdd = new Deliverable();
                sdd.setName("SDD Document");
                sdd.setStage("Final");
                deliverableRepository.save(sdd);
            }

            // Initialize a sample group if none exist
            if (projectGroupRepository.count() == 0 && adviser != null && student != null) {
                ProjectGroup groupAlpha = new ProjectGroup();
                groupAlpha.setCode("GRP-ALPHA");
                groupAlpha.setTitle("Group Alpha - AI Analytics");
                groupAlpha.setAdviser(adviser);
                projectGroupRepository.save(groupAlpha);

                // Assign the sample student to this group
                student.setGroup(groupAlpha);
                userRepository.save(student);
                System.out.println("Sample group created: " + groupAlpha.getCode());
            }

            // Create a sample deadline if none exist
            if (deadlineRepository.count() == 0) {
                Deliverable proposal = deliverableRepository.findAll().stream().findFirst().orElse(null);
                if (proposal != null) {
                    Deadline deadline = new Deadline();
                    deadline.setDeliverable(proposal);
                    deadline.setDueAt(LocalDateTime.now().plusDays(7));
                    deadline.setAcademicTerm("2025-2026 First Semester");
                    deadlineRepository.save(deadline);
                }
            }
        } catch (Exception e) {
            System.err.println("Critical Error during data initialization: " + e.getMessage());
            e.printStackTrace();
        }
    }
}