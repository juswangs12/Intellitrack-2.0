package com.intellitrack;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class IntelliTrackApplication {

    public static void main(String[] args) {
        SpringApplication.run(IntelliTrackApplication.class, args);
    }

}