package com.intellitrack.dto;

public record RiskAssessmentDto(
        int riskScore,
        String riskLevel,
        String explanation) {
}