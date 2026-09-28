package com.intellitrack.config.google;

import org.springframework.context.annotation.Configuration;

@Configuration
public class GoogleCloudConfiguration {
    // Intentionally empty.
    // Google integration is initialized lazily inside GoogleSheetsService so the
    // application can start even when Google credentials are not configured.
}
