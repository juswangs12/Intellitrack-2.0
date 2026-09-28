package com.intellitrack.dto.coordinator;

import java.time.LocalDateTime;

public record StudentDeliverableCellDto(
    Long deliverableId,
    String status,       // SUBMITTED_ON_TIME, SUBMITTED_LATE, DID_NOT_SUBMIT, DUE_TODAY, UPCOMING, NOT_YET_ASSIGNED
    Integer lateDays,
    LocalDateTime submittedAt,
    String displayText,  // e.g., "Submitted ✔", "Late 5 days"
    String aiStatus      // PENDING, PROCESSING, COMPLETED, FAILED, RATE_LIMITED — null when no submission
) {}
