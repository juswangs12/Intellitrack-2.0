package com.intellitrack.dto;

import java.util.List;
import java.util.Map;

public record SystemConfigDto(
        SystemInfoDto system,
        Map<String, String> settings) {

    public record SystemInfoDto(
            String application,
            String environment,
            String javaVersion,
            String springBoot,
            List<String> activeProfiles,
            String databaseProduct,
            String datasourceUrl) {
    }
}

