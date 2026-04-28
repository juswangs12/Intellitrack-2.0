package com.intellitrack.dto;

public record GroupProgressDto(
        Long groupId,
        String groupCode,
        String groupTitle,
        long submittedCount,
        long totalDeliverables,
        double completionRate) {
}