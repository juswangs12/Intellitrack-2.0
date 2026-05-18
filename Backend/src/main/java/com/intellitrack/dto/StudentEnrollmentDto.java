package com.intellitrack.dto;

public class StudentEnrollmentDto {
    private Long id;
    private String studentId;
    private String fullName;
    private String email;
    private Long userId;
    private String subjectName;
    private String subjectCode;
    private String sectionName;

    public StudentEnrollmentDto() {}

    public StudentEnrollmentDto(Long id, String studentId, String fullName, String email, Long userId, String subjectName, String subjectCode, String sectionName) {
        this.id = id;
        this.studentId = studentId;
        this.fullName = fullName;
        this.email = email;
        this.userId = userId;
        this.subjectName = subjectName;
        this.subjectCode = subjectCode;
        this.sectionName = sectionName;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getStudentId() {
        return studentId;
    }

    public void setStudentId(String studentId) {
        this.studentId = studentId;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getSubjectName() {
        return subjectName;
    }

    public void setSubjectName(String subjectName) {
        this.subjectName = subjectName;
    }

    public String getSubjectCode() {
        return subjectCode;
    }

    public void setSubjectCode(String subjectCode) {
        this.subjectCode = subjectCode;
    }

    public String getSectionName() {
        return sectionName;
    }

    public void setSectionName(String sectionName) {
        this.sectionName = sectionName;
    }
}
