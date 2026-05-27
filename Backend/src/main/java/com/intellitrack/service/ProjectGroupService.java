package com.intellitrack.service;

import com.intellitrack.dto.GroupRequest;
import com.intellitrack.dto.ProjectGroupDTO;
import com.intellitrack.entity.ProjectGroup;
import com.intellitrack.entity.StudentEnrollment;
import com.intellitrack.entity.Subject;
import com.intellitrack.entity.User;
import com.intellitrack.exception.ResourceNotFoundException;
import com.intellitrack.repository.AdviserFeedbackRepository;
import com.intellitrack.repository.ProjectGroupRepository;
import com.intellitrack.repository.ReminderLogRepository;
import com.intellitrack.repository.RiskAssessmentLogRepository;
import com.intellitrack.repository.StudentEnrollmentRepository;
import com.intellitrack.repository.StudentSubmissionParticipationRepository;
import com.intellitrack.repository.SubjectRepository;
import com.intellitrack.repository.SubmissionCommentRepository;
import com.intellitrack.repository.SubmissionRepository;
import com.intellitrack.repository.SubmissionVersionRepository;
import com.intellitrack.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@SuppressWarnings("null")
@Service
@Transactional
public class ProjectGroupService {

    @Autowired
    private ProjectGroupRepository projectGroupRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private StudentEnrollmentRepository studentEnrollmentRepository;

    @Autowired
    private SubjectRepository subjectRepository;

    @Autowired
    private SubmissionRepository submissionRepository;

    @Autowired
    private StudentSubmissionParticipationRepository participationRepository;
    
    @Autowired
    private SubmissionVersionRepository submissionVersionRepository;
    
    @Autowired
    private SubmissionCommentRepository submissionCommentRepository;
    
    @Autowired
    private AdviserFeedbackRepository adviserFeedbackRepository;
    
    @Autowired
    private RiskAssessmentLogRepository riskAssessmentLogRepository;
    
    @Autowired
    private ReminderLogRepository reminderLogRepository;

    public List<ProjectGroupDTO> getAllGroups() {
        return projectGroupRepository.findAllWithAllRelations().stream()
                .map(ProjectGroupDTO::new)
                .collect(Collectors.toList());
    }

    public ProjectGroupDTO createGroup(GroupRequest request) {
        ProjectGroup group = new ProjectGroup();
        group.setCode(request.getCode());
        group.setTitle(request.getTitle());
        group.setCreatedAt(LocalDateTime.now());
        if (request.getSubjectCode() != null && !request.getSubjectCode().isBlank()) {
            Subject subject = subjectRepository.findByCode(request.getSubjectCode())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Subject not found with code: " + request.getSubjectCode()));
            group.setSubject(subject);
        }
        ProjectGroup saved = projectGroupRepository.save(group);
        return projectGroupRepository.findByIdWithSubject(saved.getId())
                .map(ProjectGroupDTO::new)
                .orElse(new ProjectGroupDTO(saved));
    }

    public ProjectGroupDTO updateGroup(Long groupId, GroupRequest request) {
        ProjectGroup group = projectGroupRepository.findById(groupId)
                .orElseThrow(() -> new ResourceNotFoundException("Group not found"));

        if (request.getCode() != null) {
            group.setCode(request.getCode());
        }
        if (request.getTitle() != null) {
            group.setTitle(request.getTitle());
        }
        if (request.getSubjectCode() != null && !request.getSubjectCode().isBlank()) {
            Subject subject = subjectRepository.findByCode(request.getSubjectCode())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Subject not found with code: " + request.getSubjectCode()));
            group.setSubject(subject);
        }

        ProjectGroup saved = projectGroupRepository.save(group);
        return new ProjectGroupDTO(saved);
    }

    public ProjectGroupDTO assignAdviser(Long groupId, Long adviserId) {
        ProjectGroup group = projectGroupRepository.findById(groupId)
                .orElseThrow(() -> new ResourceNotFoundException("Group not found"));

        User adviser = userRepository.findById(adviserId)
                .orElseThrow(() -> new ResourceNotFoundException("Adviser not found"));

        if (!"adviser".equals(adviser.getRole())) {
            throw new RuntimeException("User is not an adviser");
        }

        group.setAdviser(adviser);
        return new ProjectGroupDTO(projectGroupRepository.save(group));
    }

    @Deprecated
    public void addStudentToGroup(Long groupId, Long studentId) {
        projectGroupRepository.findById(groupId)
                .orElseThrow(() -> new ResourceNotFoundException("Group not found"));

        userRepository.findById(studentId)
                .orElseThrow(() -> new ResourceNotFoundException("Student not found"));

        throw new RuntimeException("Use assignStudents with enrollment IDs instead");
    }

    @Deprecated
    public void removeStudentFromGroup(Long studentId) {
        throw new RuntimeException("Use removeStudents with enrollment IDs instead");
    }

    public void deleteGroup(Long groupId) {
        try {
            System.out.println("=== Starting group deletion for groupId: " + groupId + " ===");
            
            ProjectGroup group = projectGroupRepository.findById(groupId)
                    .orElseThrow(() -> new ResourceNotFoundException("Group not found"));

            System.out.println("  - Found group: " + group.getTitle());
            
            // 0. First, clear User.group references for all users in this group
            System.out.println("  - Clearing User.group references...");
            var usersInGroup = userRepository.findByGroup_Id(groupId);
            for (var user : usersInGroup) {
                user.setGroup(null);
                userRepository.save(user);
            }
            System.out.println("    - Cleared " + usersInGroup.size() + " user references");

            // 1. First, clear and save student associations
            System.out.println("  - Clearing student associations...");
            group.getStudents().clear();
            projectGroupRepository.save(group);

            // 2. Delete all risk assessment logs for this group
            System.out.println("  - Deleting risk assessment logs...");
            var riskLogs = riskAssessmentLogRepository.findByGroup_Id(groupId);
            riskAssessmentLogRepository.deleteAll(riskLogs);
            System.out.println("    - Deleted " + riskLogs.size() + " risk assessment logs");
            
            // 3. Delete all reminder logs for this group
            System.out.println("  - Deleting reminder logs...");
            var reminderLogs = reminderLogRepository.findByGroup_Id(groupId);
            reminderLogRepository.deleteAll(reminderLogs);
            System.out.println("    - Deleted " + reminderLogs.size() + " reminder logs");
            
            // 4. Delete all submission versions for this group
            System.out.println("  - Deleting submission versions...");
            var submissionVersions = submissionVersionRepository.findBySubmission_Group_Id(groupId);
            submissionVersionRepository.deleteAll(submissionVersions);
            System.out.println("    - Deleted " + submissionVersions.size() + " submission versions");
            
            // 5. Delete all submission comments for this group
            System.out.println("  - Deleting submission comments...");
            var submissionComments = submissionCommentRepository.findBySubmission_Group_Id(groupId);
            submissionCommentRepository.deleteAll(submissionComments);
            System.out.println("    - Deleted " + submissionComments.size() + " submission comments");
            
            // 6. Delete all adviser feedback for this group
            System.out.println("  - Deleting adviser feedback...");
            var adviserFeedbacks = adviserFeedbackRepository.findBySubmission_Group_Id(groupId);
            adviserFeedbackRepository.deleteAll(adviserFeedbacks);
            System.out.println("    - Deleted " + adviserFeedbacks.size() + " adviser feedbacks");
            
            // 7. Get all submissions for this group
            System.out.println("  - Getting submissions...");
            List<com.intellitrack.entity.Submission> submissions = submissionRepository.findByGroupId(groupId);
            System.out.println("    - Found " + submissions.size() + " submissions");
            
            // 8. Delete all student submission participations first
            System.out.println("  - Deleting student participation records...");
            for (var submission : submissions) {
                var participations = participationRepository.findBySubmission_Id(submission.getId());
                participationRepository.deleteAll(participations);
            }
            
            // 9. Now delete all submissions
            System.out.println("  - Deleting submissions...");
            submissionRepository.deleteAll(submissions);

            // 10. Finally delete the group itself
            System.out.println("  - Deleting group...");
            projectGroupRepository.delete(group);
            
            System.out.println("=== Group deletion completed successfully! ===");
            
        } catch (Exception e) {
            System.err.println("=== ERROR deleting group: " + e.getMessage() + " ===");
            e.printStackTrace();
            throw new RuntimeException("Failed to delete group: " + e.getMessage(), e);
        }
    }

    public ProjectGroupDTO assignStudents(Long groupId, List<Long> enrollmentIds) {
        ProjectGroup group = projectGroupRepository.findById(groupId)
                .orElseThrow(() -> new ResourceNotFoundException("Group not found"));

        List<StudentEnrollment> enrollments = studentEnrollmentRepository.findAllById(enrollmentIds);
        Set<Long> currentEnrollmentIds = group.getStudents().stream()
                .map(StudentEnrollment::getId)
                .collect(Collectors.toSet());

        List<StudentEnrollment> enrollmentsToAdd = new ArrayList<>();

        // Determine the subject for the group
        Subject groupSubject = group.getSubject();
        
        // Check for IT332 and IT411 conflict in existing students
        boolean hasIT332 = group.getStudents().stream()
                .anyMatch(e -> e.getClassSection() != null && e.getClassSection().getSubject() != null 
                        && "IT332".equals(e.getClassSection().getSubject().getCode()));
        boolean hasIT411 = group.getStudents().stream()
                .anyMatch(e -> e.getClassSection() != null && e.getClassSection().getSubject() != null 
                        && "IT411".equals(e.getClassSection().getSubject().getCode()));
        
        for (StudentEnrollment enrollment : enrollments) {
            if (currentEnrollmentIds.contains(enrollment.getId())) {
                continue;
            }

            // Get the subject for this enrollment
            Subject enrollmentSubject = enrollment.getClassSection().getSubject();
            String subjectCode = enrollmentSubject.getCode();
            
            // Check IT332 and IT411 conflict
            if ("IT332".equals(subjectCode) && hasIT411) {
                throw new RuntimeException(
                        String.format("Cannot add IT332 student %s to a group that already has IT411 students",
                                enrollment.getFullName()));
            }
            if ("IT411".equals(subjectCode) && hasIT332) {
                throw new RuntimeException(
                        String.format("Cannot add IT411 student %s to a group that already has IT332 students",
                                enrollment.getFullName()));
            }
            
            // Set group subject if not already set
            if (groupSubject == null) {
                groupSubject = enrollmentSubject;
                group.setSubject(groupSubject);
            } else {
                // Validate that all enrollments share the same subject
                if (!groupSubject.getId().equals(enrollmentSubject.getId())) {
                    throw new RuntimeException(
                            String.format("Student %s is enrolled in subject %s, but group %s is for subject %s",
                                    enrollment.getFullName(),
                                    enrollmentSubject.getCode(),
                                    group.getCode(),
                                    groupSubject.getCode()));
                }
            }

            ProjectGroup existingGroup = projectGroupRepository.findByStudents_Id(enrollment.getId()).orElse(null);
            if (existingGroup != null && !existingGroup.getId().equals(groupId)) {
                throw new RuntimeException(
                        String.format("Student %s is already assigned to another group: %s",
                                enrollment.getFullName(),
                                existingGroup.getCode()));
            }

            // Update flags for next iterations
            if ("IT332".equals(subjectCode)) {
                hasIT332 = true;
            }
            if ("IT411".equals(subjectCode)) {
                hasIT411 = true;
            }

            enrollmentsToAdd.add(enrollment);
        }

        group.getStudents().addAll(enrollmentsToAdd);

        // Also update the User.group field for each student
        for (StudentEnrollment enrollment : enrollmentsToAdd) {
            if (enrollment.getStudent() != null) {
                User student = enrollment.getStudent();
                student.setGroup(group);
                userRepository.save(student);
            }
        }

        return new ProjectGroupDTO(projectGroupRepository.save(group));
    }

    public ProjectGroupDTO removeStudents(Long groupId, List<Long> enrollmentIds) {
        ProjectGroup group = projectGroupRepository.findById(groupId)
                .orElseThrow(() -> new ResourceNotFoundException("Group not found"));

        Set<Long> enrollmentIdsToRemove = Set.copyOf(enrollmentIds);
        List<StudentEnrollment> studentsToRemove = group.getStudents().stream()
                .filter(e -> enrollmentIdsToRemove.contains(e.getId()))
                .toList();

        group.getStudents().removeIf(e -> enrollmentIdsToRemove.contains(e.getId()));

        // Also clear the User.group field for each removed student
        for (StudentEnrollment enrollment : studentsToRemove) {
            if (enrollment.getStudent() != null) {
                User student = enrollment.getStudent();
                student.setGroup(null);
                userRepository.save(student);
            }
        }

        return new ProjectGroupDTO(projectGroupRepository.save(group));
    }
}
