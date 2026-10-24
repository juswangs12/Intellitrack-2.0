package com.intellitrack;

import io.github.cdimascio.dotenv.Dotenv;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableCaching
@EnableScheduling
public class IntelliTrackApplication {

    public static void main(String[] args) {
        Dotenv dotenv = Dotenv.configure().ignoreIfMissing().load();
        dotenv.entries(Dotenv.Filter.DECLARED_IN_ENV_FILE).forEach(entry -> {
            if(System.getenv(entry.getKey())==null && System.getProperty(entry.getKey())==null)
                System.setProperty(entry.getKey(), entry.getValue());
        });
        SpringApplication.run(IntelliTrackApplication.class, args);
    }

}
