package com.intellitrack.dto;

import java.time.LocalDateTime;

public record SubmissionCommentDto(
        Long id,
        String content,
        LocalDateTime createdAt,
        Long authorId,
        String authorFirstName,
        String authorLastName) {
}

