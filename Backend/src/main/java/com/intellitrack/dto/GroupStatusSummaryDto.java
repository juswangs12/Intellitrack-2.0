package com.intellitrack.dto;

public record GroupStatusSummaryDto(
                Long groupId,
                String groupCode,
                String groupTitle,
                long submittedCount,
                long pendingCount,
                long lateCount,
                long totalDeliverables) {
}