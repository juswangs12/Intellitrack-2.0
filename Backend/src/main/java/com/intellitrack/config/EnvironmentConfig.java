package com.intellitrack.config;

import io.github.cdimascio.dotenv.Dotenv;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;

import java.util.HashMap;
import java.util.Map;

@Configuration
public class EnvironmentConfig {

    @Bean
    public Dotenv dotenv(ConfigurableEnvironment environment) {
        System.out.println("=== EnvironmentConfig.dotenv called ===");
        Dotenv dotenv = Dotenv.configure()
                .ignoreIfMissing()
                .load();
        
        System.out.println("Loaded .env entries:");
        dotenv.entries().forEach(entry -> {
            if (!entry.getKey().contains("SECRET") && !entry.getKey().contains("PASSWORD") && !entry.getKey().contains("KEY")) {
                System.out.println("  " + entry.getKey() + ": " + entry.getValue());
            } else {
                System.out.println("  " + entry.getKey() + ": [HIDDEN]");
            }
        });

        // Convert .env entries to Spring properties (UPPER_SNAKE_CASE → lowercase.with.dots)
        Map<String, Object> envMap = new HashMap<>();
        dotenv.entries().forEach(entry -> {
            // Add both original key and converted key
            envMap.put(entry.getKey(), entry.getValue());
            String springKey = convertToSpringPropertyKey(entry.getKey());
            if (!springKey.equals(entry.getKey())) {
                envMap.put(springKey, entry.getValue());
                System.out.println("  Mapped " + entry.getKey() + " → " + springKey);
            }
        });

        // Add to Spring environment
        environment.getPropertySources().addFirst(
            new MapPropertySource("dotenv", envMap)
        );
        
        System.out.println("=== EnvironmentConfig.dotenv complete ===");

        return dotenv;
    }
    
    private String convertToSpringPropertyKey(String envKey) {
        // Convert AI_API_KEY → ai.api.key
        return envKey.toLowerCase().replace("_", ".");
    }
}