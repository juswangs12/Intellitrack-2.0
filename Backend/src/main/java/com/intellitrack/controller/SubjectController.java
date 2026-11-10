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
    @org.springframework.beans.factory.annotation.Autowired private com.intellitrack.service.SemesterService semesters;

    @Autowired
    private SubjectRepository subjectRepository;

    @Autowired
    private ClassSectionRepository classSectionRepository;

    @Autowired
    private StudentEnrollmentRepository studentEnrollmentRepository;

    @Autowired
    private UserRepository userRepository;
    @Autowired private com.intellitrack.repository.ProjectGroupRepository groupRepository;
    @Autowired private com.intellitrack.repository.StudentSubmissionParticipationRepository participationRepository;
    @Autowired private com.intellitrack.repository.SubmissionRepository submissionRepository;

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
                sectionName).withProgression(enrollment).withAcademicTerm(enrollment.getClassSection() == null ? null : enrollment.getClassSection().getAcademicTerm());
    }

    private ClassSectionDto toSectionDto(ClassSection section) {
        List<StudentEnrollment> enrollments = studentEnrollmentRepository
                .findByClassSectionIdWithSectionAndSubject(section.getId());
        return new ClassSectionDto(
                section.getId(),
                section.getSection() + " · " + (section.getAcademicTerm() == null ? "Unassigned term" : section.getAcademicTerm()),
                enrollments.stream().map(this::toEnrollmentDto).collect(Collectors.toList())).withAcademicTerm(section.getAcademicTerm()).withSemesterArchived(semesters.archived(section.getAcademicTerm()));
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
        semesters.requireActive(classSectionRepository.findById(sectionId).orElseThrow().getAcademicTerm());
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
        if (!"student".equals(user.getRole())) throw new IllegalArgumentException("Only student accounts can be enrolled");
        if (studentEnrollmentRepository.findAllWithSectionAndSubject().stream().anyMatch(e ->
                ((e.getStudent()!=null && e.getStudent().getId().equals(userId))
                    || (user.getEmail()!=null && !user.getEmail().isBlank() && user.getEmail().equalsIgnoreCase(e.getEmail()))
                    || (user.getStudentId()!=null && !user.getStudentId().isBlank() && user.getStudentId().equals(e.getStudentId())))
                && e.getClassSection().getSubject().getId().equals(subjectId)
                && java.util.Objects.equals(e.getClassSection().getAcademicTerm(), section.getAcademicTerm()))) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(ApiResponse.error("A matching enrollment already exists in this subject and term. Link the existing imported record, or resolve its identity details, instead of adding a duplicate."));
        }
        StudentEnrollment enrollment = new StudentEnrollment();
        enrollment.setStudent(user);
        com.intellitrack.service.CapstoneCoursePolicy.section(section.getSubject().getCode(),section.getAcademicTerm(),section.getSection());
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
        semesters.requireActive(enrollment.getClassSection().getAcademicTerm());
        boolean hasSubmissionHistory = participationRepository.existsByEnrollment_Id(enrollmentId)
                || enrollment.getFormerGroupIds().stream().anyMatch(id -> !submissionRepository.findByGroupId(id).isEmpty());
        if (hasSubmissionHistory)
            throw new IllegalArgumentException("This student has submission history, so the enrollment cannot be deleted. Remove them from their group to stop future group work; their history will remain available.");
        if (enrollment.getPreviousEnrollmentId() != null || studentEnrollmentRepository.findAll().stream()
                .anyMatch(e -> java.util.Objects.equals(e.getPreviousEnrollmentId(), enrollmentId)))
            throw new IllegalArgumentException("This enrollment is part of academic progression history and cannot be deleted.");
        // Removing an unused enrollment also clears its current group membership and former-group markers.
        java.util.Set<Long> removedGroupIds = new java.util.HashSet<>();
        for (var group : groupRepository.findAllWithAllRelations()) {
            if (group.getStudents().stream().anyMatch(e -> java.util.Objects.equals(e.getId(), enrollmentId))) {
                if (!submissionRepository.findByGroupId(group.getId()).isEmpty())
                    throw new IllegalArgumentException("This student's group has submission history. Remove them from their group; the enrollment record must remain to preserve history.");
                group.getStudents().removeIf(e -> java.util.Objects.equals(e.getId(), enrollmentId));
                groupRepository.save(group);
                removedGroupIds.add(group.getId());
            }
        }
        if (enrollment.getStudent() != null && enrollment.getStudent().getGroup() != null
                && removedGroupIds.contains(enrollment.getStudent().getGroup().getId()))
            enrollment.getStudent().setGroup(null);
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

        // A correction may change subject/term, but must not relabel academic history.
        Long sourceSubjectId = enrollment.getClassSection().getSubject().getId();
        Long targetSubjectId = targetSection.getSubject().getId();
        com.intellitrack.service.CapstoneCoursePolicy.name(targetSection.getSubject().getCode(),targetSection.getSubject().getName());
        if (!sourceSubjectId.equals(targetSubjectId) || !java.util.Objects.equals(enrollment.getClassSection().getAcademicTerm(),targetSection.getAcademicTerm())) {
            if(groupRepository.existsByStudents_Id(enrollmentId) || !enrollment.getFormerGroupIds().isEmpty()
                || enrollment.getPreviousEnrollmentId()!=null || studentEnrollmentRepository.findAll().stream().anyMatch(e->java.util.Objects.equals(e.getPreviousEnrollmentId(),enrollmentId)))
                throw new IllegalArgumentException("This enrollment has group or progression history. Preserve it and add a separate enrollment in the correct subject/term; do not move its history.");
        }

        boolean duplicate=studentEnrollmentRepository.findAllWithSectionAndSubject().stream().anyMatch(e->!e.getId().equals(enrollmentId)
            && e.getClassSection().getSubject().getId().equals(targetSubjectId)
            && java.util.Objects.equals(e.getClassSection().getAcademicTerm(),targetSection.getAcademicTerm())
            && ((enrollment.getStudent()!=null && e.getStudent()!=null && enrollment.getStudent().getId().equals(e.getStudent().getId()))
                || (enrollment.getStudentId()!=null && !enrollment.getStudentId().isBlank() && enrollment.getStudentId().equals(e.getStudentId()))
                || (enrollment.getEmail()!=null && !enrollment.getEmail().isBlank() && enrollment.getEmail().equalsIgnoreCase(e.getEmail()))));
        if(duplicate) return ResponseEntity.status(HttpStatus.CONFLICT).body(ApiResponse.error("Student is already enrolled in the target subject and term. Review that enrollment instead."));

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

        semesters.requireActive(enrollment.getClassSection().getAcademicTerm());semesters.requireActive(targetSection.getAcademicTerm());
        com.intellitrack.service.CapstoneCoursePolicy.section(targetSection.getSubject().getCode(),targetSection.getAcademicTerm(),targetSection.getSection());
        enrollment.setClassSection(targetSection);
        StudentEnrollment saved = studentEnrollmentRepository.save(enrollment);
        return ResponseEntity.ok(ApiResponse.success("Student moved successfully", toEnrollmentDto(saved)));
    }
}
