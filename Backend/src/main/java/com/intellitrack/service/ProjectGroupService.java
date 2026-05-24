package com.intellitrack.service;

import com.intellitrack.dto.ProjectGroupDTO;
import com.intellitrack.entity.ProjectGroup;
import com.intellitrack.entity.StudentEnrollment;
import com.intellitrack.entity.Subject;
import com.intellitrack.entity.User;
import com.intellitrack.exception.ResourceNotFoundException;
import com.intellitrack.repository.ProjectGroupRepository;
import com.intellitrack.repository.StudentEnrollmentRepository;
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

    public List<ProjectGroupDTO> getAllGroups() {
        return projectGroupRepository.findAll().stream()
                .map(ProjectGroupDTO::new)
                .collect(Collectors.toList());
    }

    public ProjectGroupDTO createGroup(ProjectGroup group) {
        group.setCreatedAt(LocalDateTime.now());
        ProjectGroup saved = projectGroupRepository.save(group);
        return new ProjectGroupDTO(saved);
    }

    public ProjectGroupDTO updateGroup(Long groupId, ProjectGroup groupDetails) {
        ProjectGroup group = projectGroupRepository.findById(groupId)
                .orElseThrow(() -> new ResourceNotFoundException("Group not found"));

        if (groupDetails.getCode() != null) {
            group.setCode(groupDetails.getCode());
        }
        if (groupDetails.getTitle() != null) {
            group.setTitle(groupDetails.getTitle());
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
        ProjectGroup group = projectGroupRepository.findById(groupId)
                .orElseThrow(() -> new ResourceNotFoundException("Group not found"));

        group.getStudents().clear();
        projectGroupRepository.save(group);

        projectGroupRepository.delete(group);
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
        
        for (StudentEnrollment enrollment : enrollments) {
            if (currentEnrollmentIds.contains(enrollment.getId())) {
                continue;
            }

            // Get the subject for this enrollment
            Subject enrollmentSubject = enrollment.getClassSection().getSubject();
            
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
