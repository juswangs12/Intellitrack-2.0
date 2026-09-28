package com.intellitrack.dto.coordinator;

import java.util.List;

public record StudentAnalyticsRowDto(
    Long enrollmentId,
    String fullName,
    String studentId,
    String section,
    String subject,
    String groupName, // null if not assigned
    String adviserName, // null if no group/adviser
    String accountStatus, // LINKED, NOT_LINKED, NEVER_REGISTERED
    Long userId, // null if not linked
    List<StudentDeliverableCellDto> deliverableStatuses,
    Double completionRate,
    Long onTimeCount,
    Long lateCount,
    Long missingCount
) {}
