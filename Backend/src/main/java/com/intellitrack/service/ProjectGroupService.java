package com.intellitrack.service;

import com.intellitrack.dto.ProjectGroupDTO;
import com.intellitrack.entity.ProjectGroup;
import com.intellitrack.entity.StudentEnrollment;
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
        ProjectGroup group = projectGroupRepository.findById(groupId)
                .orElseThrow(() -> new ResourceNotFoundException("Group not found"));
        
        User student = userRepository.findById(studentId)
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

        for (StudentEnrollment enrollment : enrollments) {
            if (currentEnrollmentIds.contains(enrollment.getId())) {
                continue;
            }

            ProjectGroup existingGroup = projectGroupRepository.findByStudents_Id(enrollment.getId()).orElse(null);
            if (existingGroup != null && !existingGroup.getId().equals(groupId)) {
                throw new RuntimeException(
                    String.format("Student %s is already assigned to another group: %s",
                        enrollment.getFullName(),
                        existingGroup.getCode())
                );
            }

            enrollmentsToAdd.add(enrollment);
        }

        group.getStudents().addAll(enrollmentsToAdd);
        return new ProjectGroupDTO(projectGroupRepository.save(group));
    }

    public ProjectGroupDTO removeStudents(Long groupId, List<Long> enrollmentIds) {
        ProjectGroup group = projectGroupRepository.findById(groupId)
                .orElseThrow(() -> new ResourceNotFoundException("Group not found"));

        Set<Long> enrollmentIdsToRemove = Set.copyOf(enrollmentIds);
        group.getStudents().removeIf(e -> enrollmentIdsToRemove.contains(e.getId()));

        return new ProjectGroupDTO(projectGroupRepository.save(group));
    }
}
