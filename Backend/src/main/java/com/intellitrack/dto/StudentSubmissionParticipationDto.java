package com.intellitrack.dto;

import com.intellitrack.entity.ParticipationStatus;
import java.time.LocalDateTime;

public record StudentSubmissionParticipationDto(
        Long id,
        Long submissionId,
        Long studentId,
        String studentName,
        Long enrollmentId,
        String subjectCode,
        String sectionName,
        ParticipationStatus status,
        boolean contributed,
        LocalDateTime submittedAt) {
}
