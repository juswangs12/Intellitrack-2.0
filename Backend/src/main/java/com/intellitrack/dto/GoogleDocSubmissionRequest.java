package com.intellitrack.dto;

public record GoogleDocSubmissionRequest(
        String googleDocUrl,
        Long groupId,
        Long deliverableId,
        String notes) {
}
