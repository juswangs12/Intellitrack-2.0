package com.intellitrack.dto;

import java.time.LocalDateTime;

public class AdviserDeliverableDto {

    private Long id;
    private String name;
    private String stage;
    private LocalDateTime dueAt;

    private String status;

    private LocalDateTime submittedAt;

    private Long submissionId;

    public AdviserDeliverableDto(
            Long id,
            String name,
            String stage,
            LocalDateTime dueAt,
            String status,
            LocalDateTime submittedAt,
            Long submissionId) {

        this.id = id;
        this.name = name;
        this.stage = stage;
        this.dueAt = dueAt;
        this.status = status;
        this.submittedAt = submittedAt;
        this.submissionId = submissionId;
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getStage() {
        return stage;
    }

    public LocalDateTime getDueAt() {
        return dueAt;
    }

    public String getStatus() {
        return status;
    }

    public LocalDateTime getSubmittedAt() {
        return submittedAt;
    }

    public Long getSubmissionId() {
        return submissionId;
    }
}