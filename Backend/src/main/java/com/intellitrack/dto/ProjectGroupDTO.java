package com.intellitrack.dto;

import com.intellitrack.entity.ProjectGroup;
import com.intellitrack.entity.StudentEnrollment;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

public class ProjectGroupDTO {
    private boolean semesterArchived;
    public boolean isSemesterArchived(){return semesterArchived;}
    public ProjectGroupDTO withSemesterArchived(boolean value){semesterArchived=value;return this;}
    private String academicTerm;
    public String getAcademicTerm() { return academicTerm; }
    private Long id;
    private String code;
    private String title;
    private Long adviserId;
    private String adviserName;
    private UserDTO adviser;
    private List<StudentEnrollmentDto> students;
    private Integer memberCount;
    private LocalDateTime createdAt;
    private Long subjectId;
    private String subjectCode;
    private String subjectName;

    public ProjectGroupDTO() {}

    public ProjectGroupDTO(ProjectGroup group) {
        this.academicTerm=group.getAcademicTerm();
        this.id = group.getId();
        this.code = group.getCode();
        this.title = group.getTitle();
        if (group.getAdviser() != null) {
            this.adviserId = group.getAdviser().getId();
            this.adviserName = group.getAdviser().getFirstName() + " " + group.getAdviser().getLastName();
            this.adviser = new UserDTO(group.getAdviser());
        }
        if (group.getSubject() != null) {
            this.subjectId = group.getSubject().getId();
            this.subjectCode = group.getSubject().getCode();
            this.subjectName = group.getSubject().getName();
        }
        if (group.getStudents() != null) {
            this.students = group.getStudents().stream()
                    .map(this::toEnrollmentDto)
                    .collect(Collectors.toList());
            this.memberCount = this.students.size();
        } else {
            this.memberCount = 0;
        }
        this.createdAt = group.getCreatedAt();
    }

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
        ).withProgression(enrollment).withAcademicTerm(enrollment.getClassSection()==null?null:enrollment.getClassSection().getAcademicTerm());
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public Long getAdviserId() { return adviserId; }
    public void setAdviserId(Long adviserId) { this.adviserId = adviserId; }
    public String getAdviserName() { return adviserName; }
    public void setAdviserName(String adviserName) { this.adviserName = adviserName; }
    public UserDTO getAdviser() { return adviser; }
    public void setAdviser(UserDTO adviser) { this.adviser = adviser; }
    public List<StudentEnrollmentDto> getStudents() { return students; }
    public void setStudents(List<StudentEnrollmentDto> students) { this.students = students; }
    public Integer getMemberCount() { return memberCount; }
    public void setMemberCount(Integer memberCount) { this.memberCount = memberCount; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public Long getSubjectId() { return subjectId; }
    public void setSubjectId(Long subjectId) { this.subjectId = subjectId; }
    public String getSubjectCode() { return subjectCode; }
    public void setSubjectCode(String subjectCode) { this.subjectCode = subjectCode; }
    public String getSubjectName() { return subjectName; }
    public void setSubjectName(String subjectName) { this.subjectName = subjectName; }
}
