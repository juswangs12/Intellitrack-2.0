package com.intellitrack.entity;

public enum SubmissionStatus {
    PENDING,
    SUBMITTED, // legacy value — treated as SUBMITTED_ON_TIME
    SUBMITTED_ON_TIME,
    SUBMITTED_LATE,
    NOT_SUBMITTED,
    COMMENTED;

    /**
     * Normalizes the legacy SUBMITTED value to SUBMITTED_ON_TIME so all
     * downstream checks only need to compare against the canonical values.
     */
    public SubmissionStatus normalize() {
        return this == SUBMITTED ? SUBMITTED_ON_TIME : this;
    }
}