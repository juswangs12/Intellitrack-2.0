package com.intellitrack.config;

import io.github.cdimascio.dotenv.Dotenv;
import org.springframework.context.annotation.*;
import org.springframework.core.env.*;
import java.util.HashMap;
import java.util.Map;

/** Local fallback only. Never log values or override deployment/test settings. */
@Configuration
@Profile("!test")
public class EnvironmentConfig {
    @Bean public Dotenv dotenv(ConfigurableEnvironment environment) {
        var dotenv=Dotenv.configure().ignoreIfMissing().load();
        Map<String,Object> values=new HashMap<>();
        dotenv.entries(Dotenv.Filter.DECLARED_IN_ENV_FILE).forEach(e->values.put(e.getKey(),e.getValue()));
        environment.getPropertySources().addLast(new MapPropertySource("dotenv",values));
        return dotenv;
    }
}
