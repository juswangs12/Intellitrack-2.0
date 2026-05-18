package com.intellitrack.service;

import com.intellitrack.dto.SystemConfigDto;
import com.intellitrack.entity.SystemSetting;
import com.intellitrack.repository.SystemSettingRepository;
import org.springframework.boot.SpringBootVersion;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.sql.DataSource;
import java.sql.Connection;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class SystemConfigService {

    private final SystemSettingRepository systemSettingRepository;
    private final Environment environment;
    private final DataSource dataSource;

    public SystemConfigService(SystemSettingRepository systemSettingRepository, Environment environment, DataSource dataSource) {
        this.systemSettingRepository = systemSettingRepository;
        this.environment = environment;
        this.dataSource = dataSource;
    }

    @Transactional
    public SystemConfigDto getConfig() {
        ensureDefaultSettings();

        Map<String, String> settings = new LinkedHashMap<>();
        for (SystemSetting setting : systemSettingRepository.findAll()) {
            settings.put(setting.getConfigKey(), setting.getConfigValue());
        }

        String datasourceUrl = environment.getProperty("spring.datasource.url", "");

        SystemConfigDto.SystemInfoDto info = new SystemConfigDto.SystemInfoDto(
                "IntelliTrack",
                environment.getProperty("spring.profiles.active", "development"),
                System.getProperty("java.version"),
                SpringBootVersion.getVersion(),
                List.of(environment.getActiveProfiles()),
                readDatabaseProductName(),
                sanitizeDatasourceUrl(datasourceUrl));

        return new SystemConfigDto(info, settings);
    }

    @Transactional
    public SystemConfigDto updateSettings(Map<String, String> updates) {
        if (updates == null || updates.isEmpty()) {
            return getConfig();
        }

        LocalDateTime now = LocalDateTime.now();
        updates.forEach((key, value) -> {
            if (key == null || key.isBlank()) {
                return;
            }
            String safeValue = value == null ? "" : value;
            SystemSetting setting = systemSettingRepository.findByConfigKey(key).orElseGet(SystemSetting::new);
            setting.setConfigKey(key);
            setting.setConfigValue(safeValue);
            setting.setUpdatedAt(now);
            systemSettingRepository.save(setting);
        });

        return getConfig();
    }

    private void ensureDefaultSettings() {
        LocalDateTime now = LocalDateTime.now();
        Map<String, String> defaults = Map.of(
                "security.jwtExpiryHours", "24",
                "security.sessionTimeoutMinutes", "60",
                "notifications.emailDeadlines", "true",
                "notifications.notifyAdviserOnSubmission", "true",
                "notifications.adminAlerts", "true"
        );

        defaults.forEach((key, value) -> systemSettingRepository.findByConfigKey(key).orElseGet(() -> {
            SystemSetting setting = new SystemSetting();
            setting.setConfigKey(key);
            setting.setConfigValue(value);
            setting.setUpdatedAt(now);
            return systemSettingRepository.save(setting);
        }));
    }

    private String readDatabaseProductName() {
        try (Connection connection = dataSource.getConnection()) {
            return connection.getMetaData().getDatabaseProductName();
        } catch (Exception e) {
            return "Unknown";
        }
    }

    private String sanitizeDatasourceUrl(String datasourceUrl) {
        if (datasourceUrl == null) {
            return "";
        }
        return datasourceUrl.replaceAll("(?i)(password=)[^&]*", "$1****");
    }
}

