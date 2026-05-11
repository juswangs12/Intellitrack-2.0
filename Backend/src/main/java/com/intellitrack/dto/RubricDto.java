package com.intellitrack.dto;

import java.util.List;

public record RubricDto(
        Long id,
        String title,
        String description,
        List<RubricCriterionDto> criteria) {
}

