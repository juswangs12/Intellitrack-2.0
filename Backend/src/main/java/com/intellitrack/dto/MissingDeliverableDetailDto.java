package com.intellitrack.dto;

public class MissingDeliverableDetailDto {
    private String deliverableName;
    private int missingStudentCount;

    public MissingDeliverableDetailDto() {
    }

    public MissingDeliverableDetailDto(String deliverableName, int missingStudentCount) {
        this.deliverableName = deliverableName;
        this.missingStudentCount = missingStudentCount;
    }

    public String getDeliverableName() {
        return deliverableName;
    }

    public void setDeliverableName(String deliverableName) {
        this.deliverableName = deliverableName;
    }

    public int getMissingStudentCount() {
        return missingStudentCount;
    }

    public void setMissingStudentCount(int missingStudentCount) {
        this.missingStudentCount = missingStudentCount;
    }
}
