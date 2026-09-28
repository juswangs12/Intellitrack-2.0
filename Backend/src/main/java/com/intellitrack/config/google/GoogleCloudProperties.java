package com.intellitrack.config.google;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "google.cloud")
public class GoogleCloudProperties {
    private String serviceAccountJson;
    private String applicationName = "IntelliTrack";
    private String spreadsheetName = "IntelliTrack Institutional Analytics";
    private String driveFolderName = "IntelliTrack Analytics";

    public String getServiceAccountJson() {
        return serviceAccountJson;
    }

    public void setServiceAccountJson(String serviceAccountJson) {
        this.serviceAccountJson = serviceAccountJson;
    }

    public String getApplicationName() {
        return applicationName;
    }

    public void setApplicationName(String applicationName) {
        this.applicationName = applicationName;
    }

    public String getSpreadsheetName() {
        return spreadsheetName;
    }

    public void setSpreadsheetName(String spreadsheetName) {
        this.spreadsheetName = spreadsheetName;
    }

    public String getDriveFolderName() {
        return driveFolderName;
    }

    public void setDriveFolderName(String driveFolderName) {
        this.driveFolderName = driveFolderName;
    }
}
