package com.intellitrack.dto;

import com.intellitrack.entity.ParticipationStatus;

public record StudentParticipationDto(
        Long studentId,
        String studentName,
        String subjectCode,
        String sectionName,
        ParticipationStatus status,
        boolean contributed) {
}
