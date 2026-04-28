package com.intellitrack.dto;

public record MetricCardDto(
        String label,
        double value,
        String tone) {
}