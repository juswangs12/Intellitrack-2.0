package com.intellitrack.controller;

import com.intellitrack.dto.ApiResponse;
import com.intellitrack.dto.ClassSectionDto;
import com.intellitrack.dto.StudentEnrollmentDto;
import com.intellitrack.dto.SubjectDto;
import com.intellitrack.entity.ClassSection;
import com.intellitrack.entity.StudentEnrollment;
import com.intellitrack.entity.Subject;
import com.intellitrack.entity.User;
import com.intellitrack.exception.ResourceNotFoundException;
import com.intellitrack.repository.ClassSectionRepository;
import com.intellitrack.repository.StudentEnrollmentRepository;
import com.intellitrack.repository.SubjectRepository;
import com.intellitrack.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/subjects")
@Transactional
public class SubjectController {

    @Autowired
    private SubjectRepository subjectRepository;

    @Autowired
    private ClassSectionRepository classSectionRepository;

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
                sectionName);
    }

    private ClassSectionDto toSectionDto(ClassSection section) {
        List<StudentEnrollment> enrollments = studentEnrollmentRepository
                .findByClassSectionIdWithSectionAndSubject(section.getId());
        return new ClassSectionDto(
                section.getId(),
                section.getSection(),
                enrollments.stream().map(this::toEnrollmentDto).collect(Collectors.toList()));
    }

    private SubjectDto toSubjectDto(Subject subject) {
        List<ClassSection> sections = classSectionRepository.findBySubjectId(subject.getId());
        return new SubjectDto(
                subject.getId(),
                subject.getName(),
                subject.getCode(),
                sections.stream().map(this::toSectionDto).collect(Collectors.toList()));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<SubjectDto>>> getAllSubjects() {
        List<Subject> subjects = subjectRepository.findAll();
        List<SubjectDto> dtos = subjects.stream().map(this::toSubjectDto).collect(Collectors.toList());
        return ResponseEntity.ok(ApiResponse.success(dtos));
    }

    @GetMapping("/{subjectId}/sections")
    public ResponseEntity<ApiResponse<List<ClassSectionDto>>> getSectionsBySubject(@PathVariable Long subjectId) {
        List<ClassSection> sections = classSectionRepository.findBySubjectId(subjectId);
        List<ClassSectionDto> dtos = sections.stream().map(this::toSectionDto).collect(Collectors.toList());
        return ResponseEntity.ok(ApiResponse.success(dtos));
    }

    @GetMapping("/sections/{sectionId}/enrollments")
    public ResponseEntity<ApiResponse<List<StudentEnrollmentDto>>> getEnrollmentsBySection(
            @PathVariable Long sectionId) {
        List<StudentEnrollment> enrollments = studentEnrollmentRepository
                .findByClassSectionIdWithSectionAndSubject(sectionId);
        List<StudentEnrollmentDto> dtos = enrollments.stream().map(this::toEnrollmentDto).collect(Collectors.toList());
        return ResponseEntity.ok(ApiResponse.success(dtos));
    }

    @GetMapping("/with-sections")
    public ResponseEntity<ApiResponse<List<SubjectDto>>> getSubjectsWithSections() {
        List<Subject> subjects = subjectRepository.findAll();
        List<SubjectDto> dtos = subjects.stream().map(this::toSubjectDto).collect(Collectors.toList());
        return ResponseEntity.ok(ApiResponse.success(dtos));
    }

    @GetMapping("/enrollments")
    public ResponseEntity<ApiResponse<List<StudentEnrollmentDto>>> getAllEnrollments() {
        List<StudentEnrollment> enrollments = studentEnrollmentRepository.findAllWithSectionAndSubject();
        List<StudentEnrollmentDto> dtos = enrollments.stream().map(this::toEnrollmentDto).collect(Collectors.toList());
        return ResponseEntity.ok(ApiResponse.success(dtos));
    }

    @PostMapping("/sections/{sectionId}/enroll")
    public ResponseEntity<ApiResponse<StudentEnrollmentDto>> addStudentManually(
            @PathVariable Long sectionId,
            @RequestBody Map<String, Long> body) {
        Long userId = body.get("userId");
        if (userId == null) {
            return ResponseEntity.badRequest().body(ApiResponse.error("userId is required"));
        }
        ClassSection section = classSectionRepository.findById(sectionId)
                .orElseThrow(() -> new ResourceNotFoundException("Section not found"));
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        // Prevent duplicate enrollment in the same section
        boolean alreadyEnrolled = studentEnrollmentRepository.findByClassSectionIdWithSectionAndSubject(sectionId)
                .stream().anyMatch(e -> e.getStudent() != null && e.getStudent().getId().equals(userId));
        if (alreadyEnrolled) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(ApiResponse.error("Student is already enrolled in this section"));
        }
        // Prevent duplicate enrollment across sections of the same subject
        Long subjectId = section.getSubject().getId();
        if (studentEnrollmentRepository.existsBySubjectIdAndUserId(subjectId, userId)) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(ApiResponse.error("Student is already enrolled in another section of this subject"));
        }
        StudentEnrollment enrollment = new StudentEnrollment();
        enrollment.setStudent(user);
        enrollment.setClassSection(section);
        enrollment.setStudentId(user.getStudentId() != null ? user.getStudentId() : "");
        enrollment.setFullName(user.getFirstName() + " " + user.getLastName());
        enrollment.setEmail(user.getEmail());
        StudentEnrollment saved = studentEnrollmentRepository.save(enrollment);
        return ResponseEntity.ok(ApiResponse.success("Student enrolled successfully", toEnrollmentDto(saved)));
    }

    @DeleteMapping("/enrollments/{enrollmentId}")
    public ResponseEntity<ApiResponse<Void>> unenrollStudent(@PathVariable Long enrollmentId) {
        StudentEnrollment enrollment = studentEnrollmentRepository.findById(enrollmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Enrollment not found"));
        studentEnrollmentRepository.delete(enrollment);
        return ResponseEntity.ok(ApiResponse.success("Student unenrolled successfully", null));
    }

    @PutMapping("/enrollments/{enrollmentId}/move/{targetSectionId}")
    public ResponseEntity<ApiResponse<StudentEnrollmentDto>> moveStudent(
            @PathVariable Long enrollmentId,
            @PathVariable Long targetSectionId) {
        StudentEnrollment enrollment = studentEnrollmentRepository.findByIdWithSectionAndSubject(enrollmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Enrollment not found"));
        ClassSection targetSection = classSectionRepository.findById(targetSectionId)
                .orElseThrow(() -> new ResourceNotFoundException("Target section not found"));

        // Ensure move is within the same subject
        Long sourceSubjectId = enrollment.getClassSection().getSubject().getId();
        Long targetSubjectId = targetSection.getSubject().getId();
        if (!sourceSubjectId.equals(targetSubjectId)) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Cannot move student between different subjects"));
        }

        // Check for duplicate in the target section
        if (enrollment.getStudent() != null &&
                studentEnrollmentRepository.existsByClassSectionIdAndStudent_Id(targetSectionId, enrollment.getStudent().getId())) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(ApiResponse.error("Student is already enrolled in the target section"));
        }
        if (enrollment.getStudentId() != null && !enrollment.getStudentId().isBlank() &&
                studentEnrollmentRepository.existsByClassSectionIdAndStudentId(targetSectionId, enrollment.getStudentId())) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(ApiResponse.error("Student is already enrolled in the target section"));
        }

        enrollment.setClassSection(targetSection);
        StudentEnrollment saved = studentEnrollmentRepository.save(enrollment);
        return ResponseEntity.ok(ApiResponse.success("Student moved successfully", toEnrollmentDto(saved)));
    }
}
