package com.intellitrack.service;

import com.intellitrack.dto.SystemConfigDto;
import org.springframework.boot.SpringBootVersion;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;
import javax.sql.DataSource;
import java.util.*;

/** Reports effective runtime configuration; does not present unused DB flags as controls. */
@Service
public class SystemConfigService {
    private final Environment environment;
    private final DataSource dataSource;
    private final GoogleWorkspaceService google;
    public SystemConfigService(Environment environment,DataSource dataSource,GoogleWorkspaceService google) {
        this.environment=environment;this.dataSource=dataSource;this.google=google;
    }
    public SystemConfigDto getConfig() {
        Map<String,String> settings=new LinkedHashMap<>();
        settings.put("Access token lifetime",environment.getProperty("jwt.expiration",Long.class,86400000L)/3600000L+" hours");
        settings.put("Academic timezone","Asia/Manila");
        settings.put("Google sign-in",configured("spring.security.oauth2.client.registration.google.client-id")&&configured("spring.security.oauth2.client.registration.google.client-secret")?"Configured; verify with live sign-in":"Not configured");
        settings.put("Calendar and Reports OAuth",google.configured()?"Configured; live consent required":"Configuration incomplete");
        settings.put("AI summaries",configured("ai.api.key")?"API key configured; subject to provider availability":"Computed summaries only; AI key not configured");
        settings.put("Calendar reminders","Student-triggered sync; one day and one hour before deadline");
        String product="Unavailable";
        try(var connection=dataSource.getConnection()){product=connection.getMetaData().getDatabaseProductName();}catch(java.sql.SQLException ignored){}
        var info=new SystemConfigDto.SystemInfoDto("IntelliTrack",String.join(", ",environment.getActiveProfiles()),System.getProperty("java.version"),SpringBootVersion.getVersion(),List.of(environment.getActiveProfiles()),product,"");
        return new SystemConfigDto(info,settings);
    }
    public SystemConfigDto updateSettings(Map<String,String> ignored) {
        throw new IllegalArgumentException("Runtime settings are managed through the backend deployment environment");
    }
    private boolean configured(String key){return !environment.getProperty(key,"").isBlank();}
}
