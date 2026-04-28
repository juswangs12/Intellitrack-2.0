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
    public void runAfterStartup() {
        try {
            // Only initialize if no users exist
            if (userRepository.count() == 0) {
                BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

                // Create test users
                User adviser = new User("Jane", "Smith", "adviser@university.edu",
                        passwordEncoder.encode("password123"), "adviser");
                adviser.setDepartment("Computer Science");
                adviser.setPhone("+1234567891");
                adviser.setCreatedAt(LocalDateTime.now());
                adviser = userRepository.save(adviser);

                ProjectGroup projectGroup = new ProjectGroup();
                projectGroup.setCode("CS-2026-A");
                projectGroup.setTitle("IntelliTrack Revamp Team");
                projectGroup.setAdviser(adviser);
                projectGroup.setCreatedAt(LocalDateTime.now());
                projectGroup = projectGroupRepository.save(projectGroup);

                User student = new User("John", "Doe", "student@university.edu", passwordEncoder.encode("password123"),
                        "student");
                student.setStudentId("STU001");
                student.setDepartment("Computer Science");
                student.setYear("3");
                student.setPhone("+1234567890");
                student.setAdvisorId(adviser.getId());
                student.setGroup(projectGroup);
                student.setCreatedAt(LocalDateTime.now());
                userRepository.save(student);

                User coordinator = new User("Bob", "Johnson", "coordinator@university.edu",
                        passwordEncoder.encode("password123"), "coordinator");
                coordinator.setDepartment("Computer Science");
                coordinator.setPhone("+1234567892");
                coordinator.setCreatedAt(LocalDateTime.now());
                userRepository.save(coordinator);

                User admin = new User("Alice", "Brown", "admin@university.edu", passwordEncoder.encode("password123"),
                        "administrator");
                admin.setDepartment("Administration");
                admin.setPhone("+1234567893");
                admin.setCreatedAt(LocalDateTime.now());
                userRepository.save(admin);

                Deliverable proposal = createDeliverable("Project Proposal", "Proposal");
                Deliverable midterm = createDeliverable("Midterm Manuscript", "Midterm");
                Deliverable finalDefense = createDeliverable("Final Defense Package", "Final");

                createDeadline(proposal, LocalDateTime.now().plusDays(5));
                createDeadline(midterm, LocalDateTime.now().plusDays(12));
                createDeadline(finalDefense, LocalDateTime.now().plusDays(25));

                createSubmission(projectGroup, proposal, SubmissionStatus.SUBMITTED, LocalDateTime.now().minusDays(1),
                        1, 0, "Submitted on time");
                createSubmission(projectGroup, midterm, SubmissionStatus.PENDING, null, 1, 0, "Draft in progress");
                createSubmission(projectGroup, finalDefense, SubmissionStatus.PENDING, null, 1, 0, "No upload yet");

                System.out.println("Test users initialized successfully!");
            }
        } catch (Exception e) {
            System.err.println("Error initializing test data: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private Deliverable createDeliverable(String name, String stage) {
        Deliverable deliverable = new Deliverable();
        deliverable.setName(name);
        deliverable.setStage(stage);
        deliverable.setActive(true);
        return deliverableRepository.save(deliverable);
    }

    private void createDeadline(Deliverable deliverable, LocalDateTime dueAt) {
        Deadline deadline = new Deadline();
        deadline.setDeliverable(deliverable);
        deadline.setDueAt(dueAt);
        deadline.setAcademicTerm("2025-2026");
        deadlineRepository.save(deadline);
    }

    private void createSubmission(ProjectGroup group, Deliverable deliverable, SubmissionStatus status,
            LocalDateTime submittedAt, int versionNumber, int revisionCount, String notes) {
        Submission submission = new Submission();
        submission.setGroup(group);
        submission.setDeliverable(deliverable);
        submission.setStatus(status);
        submission.setSubmittedAt(submittedAt);
        submission.setVersionNumber(versionNumber);
        submission.setRevisionCount(revisionCount);
        submission.setNotes(notes);
        submissionRepository.save(submission);
    }
}