package com.intellitrack.dto;

import java.util.List;

public record GroupProgressDto(
                Long groupId,
                String groupCode,
                String groupTitle,
                Long subjectId,
                String subjectCode,
                String subjectName,
                long submittedCount,
                long totalDeliverables,
                double completionRate,
                long onTimeStudents,
                long lateStudents,
                long missingStudents,
                List<StudentParticipationDto> studentParticipations) {
}