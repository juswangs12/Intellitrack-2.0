package com.intellitrack.config.google;

import com.intellitrack.service.GoogleSheetsService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

@Component
public class GoogleSheetsHealthIndicator implements HealthIndicator {
    private static final Logger logger = LoggerFactory.getLogger(GoogleSheetsHealthIndicator.class);

    private final GoogleSheetsService googleSheetsService;

    public GoogleSheetsHealthIndicator(GoogleSheetsService googleSheetsService) {
        this.googleSheetsService = googleSheetsService;
    }

    @Override
    public Health health() {
        try {
            logger.info("Checking Google Sheets integration health...");
            boolean configured = googleSheetsService.isConfigured();
            if (configured) {
                logger.info("Google Sheets integration is healthy and configured!");
                return Health.up()
                        .withDetail("configured", true)
                        .withDetail("status", "Operational")
                        .build();
            } else {
                logger.warn("Google Sheets integration is NOT configured!");
                return Health.down()
                        .withDetail("configured", false)
                        .withDetail("status", "Missing Credentials")
                        .withDetail("message", "Set GOOGLE_APPLICATION_CREDENTIALS, GOOGLE_SERVICE_ACCOUNT_JSON, or provide google-service-account.json on classpath")
                        .build();
            }
        } catch (Exception e) {
            logger.error("Google Sheets health check failed", e);
            return Health.down()
                    .withDetail("error", e.getMessage())
                    .build();
        }
    }
}
