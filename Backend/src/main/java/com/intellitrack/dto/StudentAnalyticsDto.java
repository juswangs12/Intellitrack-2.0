package com.intellitrack.dto;

import java.util.List;

public record StudentAnalyticsDto(
                Long studentId,
                String studentName,
                String studentNumber,
                Long groupId,
                String groupCode,
                String groupTitle,
                String subjectCode,
                String subjectName,
                String sectionName,
                long totalDeliverables,
                long onTimeDeliverables,
                long lateDeliverables,
                long missingDeliverables,
                double complianceRate,
                List<StudentDeliverableStatusDto> deliverableStatuses) {
}
