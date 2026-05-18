package com.intellitrack.controller;

import com.intellitrack.dto.ApiResponse;
import com.intellitrack.dto.StudentEnrollmentDto;
import com.intellitrack.entity.StudentEnrollment;
import com.intellitrack.entity.User;
import com.intellitrack.exception.ResourceNotFoundException;
import com.intellitrack.repository.StudentEnrollmentRepository;
import com.intellitrack.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/student-enrollments")
@CrossOrigin(origins = "http://localhost:3000")
public class StudentEnrollmentController {

    @Autowired
    private StudentEnrollmentRepository studentEnrollmentRepository;

    @Autowired
    private UserRepository userRepository;

    private StudentEnrollmentDto toEnrollmentDto(StudentEnrollment enrollment) {
        String subjectName = null;
        String subjectCode = null;
        String sectionName = null;
        if (enrollment.getClassSection() != null) {
            sectionName = enrollment.getClassSection().getSection();
            if (enrollment.getClassSection().getSubject() != null) {
                subjectName = enrollment.getClassSection().getSubject().getName();
                subjectCode = enrollment.getClassSection().getSubject().getCode();
            }
        }
        return new StudentEnrollmentDto(
                enrollment.getId(),
                enrollment.getStudentId(),
                enrollment.getFullName(),
                enrollment.getEmail(),
                enrollment.getStudent() != null ? enrollment.getStudent().getId() : null,
                subjectName,
                subjectCode,
                sectionName
        );
    }

    @PostMapping("/{enrollmentId}/link-user")
    @Transactional
    public ResponseEntity<ApiResponse<StudentEnrollmentDto>> linkUserToEnrollment(
            @PathVariable Long enrollmentId,
            @RequestBody Map<String, Long> payload) {
        
        Long userId = payload.get("userId");
        
        StudentEnrollment enrollment = studentEnrollmentRepository.findById(enrollmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Enrollment not found"));
        
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        
        enrollment.setStudent(user);
        
        // Also update the user's studentId to match the enrollment's studentId
        if (enrollment.getStudentId() != null && !enrollment.getStudentId().isEmpty()) {
            user.setStudentId(enrollment.getStudentId());
        }
        
        // Backward compatibility: also set user.group for frontend
        if (enrollment.getGroups() != null && !enrollment.getGroups().isEmpty()) {
            user.setGroup(enrollment.getGroups().get(0));
        }
        
        userRepository.save(user);
        studentEnrollmentRepository.save(enrollment);
        
        return ResponseEntity.ok(ApiResponse.success("User linked to enrollment successfully", toEnrollmentDto(enrollment)));
    }

    @GetMapping("/unlinked")
    public ResponseEntity<ApiResponse<List<StudentEnrollmentDto>>> getUnlinkedEnrollments() {
        List<StudentEnrollmentDto> enrollments = studentEnrollmentRepository.findAll().stream()
                .filter(e -> e.getStudent() == null)
                .map(this::toEnrollmentDto)
                .toList();
        return ResponseEntity.ok(ApiResponse.success(enrollments));
    }
    
    @GetMapping("/student/{userId}")
    public ResponseEntity<ApiResponse<List<StudentEnrollmentDto>>> getEnrollmentsByStudent(@PathVariable Long userId) {
        List<StudentEnrollmentDto> enrollments = studentEnrollmentRepository.findByStudent_IdWithAllRelations(userId).stream()
                .map(this::toEnrollmentDto)
                .toList();
        return ResponseEntity.ok(ApiResponse.success(enrollments));
    }
}
