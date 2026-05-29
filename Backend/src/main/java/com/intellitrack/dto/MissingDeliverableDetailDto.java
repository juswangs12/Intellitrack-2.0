package com.intellitrack.dto;

import java.util.List;

public class MissingDeliverableDetailDto {
    private String deliverableName;
    private List<String> studentNames;

    public MissingDeliverableDetailDto() {
    }

    public MissingDeliverableDetailDto(String deliverableName, List<String> studentNames) {
        this.deliverableName = deliverableName;
        this.studentNames = studentNames;
    }

    public String getDeliverableName() {
        return deliverableName;
    }

    public void setDeliverableName(String deliverableName) {
        this.deliverableName = deliverableName;
    }

    public List<String> getStudentNames() {
        return studentNames;
    }

    public void setStudentNames(List<String> studentNames) {
        this.studentNames = studentNames;
    }
}
