package com.tausib.jobhunter.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;
import java.util.List;

/** Type-safe view of the "jobhunter:" block in application.yml. */
@ConfigurationProperties(prefix = "jobhunter")
public record JobHunterProperties(Apify apify, Discovery discovery) {

    public record Apify(String token, String baseUrl, Duration timeout, List<Source> sources) {}

    /** searches: optional per-source list (each site wants its own location format); empty = use discovery.searches. */
    public record Source(String actorId, String inputTemplate, boolean enabled, List<Search> searches) {}

    public record Discovery(
            boolean schedulerEnabled,
            String cron,
            String zone,
            int maxItemsPerSearch,
            int maxSearchesPerRun,
            int delaySecondsBetweenSearches,
            int maxYearsExperience,
            int minScoreToShortlist,
            List<String> locationMustContain,
            List<Search> searches) {}

    public record Search(String keyword, String location) {}
}
