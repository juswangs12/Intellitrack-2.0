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
        Dotenv dotenv = Dotenv.configure()
                .ignoreIfMissing()
                .load();

        // Convert .env entries to Spring properties
        Map<String, Object> envMap = new HashMap<>();
        dotenv.entries().forEach(entry -> {
            envMap.put(entry.getKey(), entry.getValue());
        });

        // Add to Spring environment
        environment.getPropertySources().addFirst(
            new MapPropertySource("dotenv", envMap)
        );

        return dotenv;
    }
}