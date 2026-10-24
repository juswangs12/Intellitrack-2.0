package com.intellitrack.dto;

import java.time.LocalDateTime;

public record TrackingRow(Long enrollmentId, Long studentId, String studentNumber, String studentName,
    String section, Long subjectId, String subject, String academicTerm,
    Long groupId, String group, Long adviserId, String adviser, Long deliverableId, String deliverable,
    LocalDateTime dueAt, Long submissionId, LocalDateTime firstSubmittedAt, LocalDateTime latestSubmittedAt,
    String status, String latestStatus, String googleDocUrl, boolean linked,
    boolean canSubmit, String submissionBlockReason) {}
