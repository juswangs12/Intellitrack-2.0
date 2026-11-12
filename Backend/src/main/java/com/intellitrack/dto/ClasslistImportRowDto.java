package com.intellitrack.dto;

public record ClasslistImportRowDto(
        int rowNumber,
        String studentId,
        String fullName,
        String email,
        String status,
        String message) {}
