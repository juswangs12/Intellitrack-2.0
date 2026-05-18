package com.intellitrack.dto;

import java.util.List;

public class ClassSectionDto {
    private Long id;
    private String section;
    private List<StudentEnrollmentDto> enrollments;

    public ClassSectionDto() {}

    public ClassSectionDto(Long id, String section, List<StudentEnrollmentDto> enrollments) {
        this.id = id;
        this.section = section;
        this.enrollments = enrollments;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getSection() {
        return section;
    }

    public void setSection(String section) {
        this.section = section;
    }

    public List<StudentEnrollmentDto> getEnrollments() {
        return enrollments;
    }

    public void setEnrollments(List<StudentEnrollmentDto> enrollments) {
        this.enrollments = enrollments;
    }
}
