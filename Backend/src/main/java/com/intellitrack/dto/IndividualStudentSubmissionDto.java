package com.intellitrack.dto;

import com.intellitrack.entity.ParticipationStatus;
import java.time.LocalDateTime;

public record IndividualStudentSubmissionDto(
        Long participationId,
        Long submissionId,
        Long studentId,
        String studentName,
        Long groupId,
        String groupCode,
        String groupTitle,
        Long deliverableId,
        String deliverableName,
        String stage,
        String submissionStatus,
        ParticipationStatus participationStatus,
        LocalDateTime submittedAt,
        String subjectCode,
        String sectionName,
        Integer lateDays,
        String fileUrl,
        String aiSummary,
        String aiHighlights) {
}
