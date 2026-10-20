package com.tausib.jobhunter;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@ConfigurationPropertiesScan
@EnableScheduling
@EnableAsync
public class JobHunterApplication {
    public static void main(String[] args) {
        // Windows JVMs report the legacy "Asia/Calcutta", which PostgreSQL 16 rejects at connect time.
        java.util.TimeZone.setDefault(java.util.TimeZone.getTimeZone("Asia/Kolkata"));
        SpringApplication.run(JobHunterApplication.class, args);
    }
}
