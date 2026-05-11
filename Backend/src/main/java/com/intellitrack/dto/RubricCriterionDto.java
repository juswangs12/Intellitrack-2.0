package com.intellitrack.dto;

public record RubricCriterionDto(
        Long id,
        String name,
        String description,
        Integer maxPoints,
        Integer weight) {
}

