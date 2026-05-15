package com.intellitrack.dto;

public class ManualEnrollmentRequest {

    private Long userId;
    private Long classSectionId;

    public ManualEnrollmentRequest() {
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public Long getClassSectionId() {
        return classSectionId;
    }

    public void setClassSectionId(Long classSectionId) {
        this.classSectionId = classSectionId;
    }
}
