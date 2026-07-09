package com.intellitrack.entity;

public enum SubmissionStatus {
    PENDING,
    NOT_SUBMITTED,
    OVERDUE,
    SUBMITTED,
    SUBMITTED_ON_TIME,
    SUBMITTED_LATE,
    COMMENTED;

    /**
     * Normalizes the legacy SUBMITTED value to SUBMITTED_ON_TIME so all
     * downstream checks only need to compare against the canonical values.
     */
    public SubmissionStatus normalize() {
        return this == SUBMITTED ? SUBMITTED_ON_TIME : this;
    }
}