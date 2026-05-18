package com.intellitrack.controller;

import com.intellitrack.dto.ApiResponse;
import com.intellitrack.dto.ClassSectionDto;
import com.intellitrack.dto.StudentEnrollmentDto;
import com.intellitrack.dto.SubjectDto;
import com.intellitrack.entity.ClassSection;
import com.intellitrack.entity.StudentEnrollment;
import com.intellitrack.entity.Subject;
import com.intellitrack.repository.ClassSectionRepository;
import com.intellitrack.repository.StudentEnrollmentRepository;
import com.intellitrack.repository.SubjectRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/subjects")
@CrossOrigin(origins = "http://localhost:3000")
@Transactional
public class SubjectController {

    @Autowired
    private SubjectRepository subjectRepository;

    @Autowired
    private ClassSectionRepository classSectionRepository;

    @Autowired
    private StudentEnrollmentRepository studentEnrollmentRepository;

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

    private ClassSectionDto toSectionDto(ClassSection section) {
        List<StudentEnrollment> enrollments = studentEnrollmentRepository.findByClassSectionIdWithSectionAndSubject(section.getId());
        return new ClassSectionDto(
                section.getId(),
                section.getSection(),
                enrollments.stream().map(this::toEnrollmentDto).collect(Collectors.toList())
        );
    }

    private SubjectDto toSubjectDto(Subject subject) {
        List<ClassSection> sections = classSectionRepository.findBySubjectId(subject.getId());
        return new SubjectDto(
                subject.getId(),
                subject.getName(),
                subject.getCode(),
                sections.stream().map(this::toSectionDto).collect(Collectors.toList())
        );
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
    public ResponseEntity<ApiResponse<List<StudentEnrollmentDto>>> getEnrollmentsBySection(@PathVariable Long sectionId) {
        List<StudentEnrollment> enrollments = studentEnrollmentRepository.findByClassSectionIdWithSectionAndSubject(sectionId);
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
}
