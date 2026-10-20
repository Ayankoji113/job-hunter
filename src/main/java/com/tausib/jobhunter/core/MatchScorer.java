package com.tausib.jobhunter.core;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Explainable job-match score (0–100) based on skills mentioned in the job text.
 *
 * score = matchedWeight / (matchedWeight + missingWeight) * 100
 *   + 10 bonus if the posting is clearly entry-level / intern
 *   capped to [0, 100]. If the JD mentions no known skills, the base is 30.
 *
 * "matched" = a skill you HAVE that the JD mentions.
 * "missing" = a skill you DON'T have that the JD mentions (useful learning signal).
 */
public final class MatchScorer {

    public record Skill(String name, Pattern pattern, int weight, boolean have) {
        public static Skill of(String name, String regex, int weight, boolean have) {
            return new Skill(name, Pattern.compile("(?i)(?<![a-z])(?:" + regex + ")(?![a-z])"), weight, have);
        }
    }

    public record Result(int score, List<String> matched, List<String> missing) {}

    private final List<Skill> skills;

    public MatchScorer(List<Skill> skills) {
        this.skills = List.copyOf(skills);
    }

    public Result score(JobListing job) {
        String text = ((job.title() == null ? "" : job.title()) + " "
                + (job.description() == null ? "" : job.description())).toLowerCase(Locale.ROOT);

        List<String> matched = new ArrayList<>();
        List<String> missing = new ArrayList<>();
        int matchedW = 0, missingW = 0;
        for (Skill s : skills) {
            if (s.pattern().matcher(text).find()) {
                if (s.have()) { matched.add(s.name()); matchedW += s.weight(); }
                else { missing.add(s.name()); missingW += s.weight(); }
            }
        }
        int base = (matchedW + missingW) == 0 ? 30 : Math.round(100f * matchedW / (matchedW + missingW));
        if (JobFilter.isEntryLevel(text)) base += 10;
        // A job that never mentions Java or Spring Boot is not a Java job, however few "missing" skills it lists.
        if (!matched.contains("Java") && !matched.contains("Spring Boot")) base = Math.min(base, 50);
        int score = Math.max(0, Math.min(100, base));
        return new Result(score, matched, missing);
    }

    /** Default profile built from Tausib's resume. Edit weights/flags as skills change. */
    public static List<Skill> defaultProfile() {
        return List.of(
                // ---- skills on the resume (have = true) ----
                Skill.of("Java", "java(?!\\s*script)|core java|java 8|java 17|java 21", 5, true),
                Skill.of("Spring Boot", "spring\\s*boot", 5, true),
                Skill.of("Spring Security", "spring security", 3, true),
                Skill.of("Spring Data JPA", "spring data|jpa", 3, true),
                Skill.of("Hibernate", "hibernate", 3, true),
                Skill.of("Spring MVC", "spring mvc|spring framework|spring core", 2, true),
                Skill.of("REST APIs", "rest(?:ful)?\\s*apis?|rest(?:ful)? services|restful|web services", 4, true),
                Skill.of("SQL", "sql|rdbms", 3, true),
                Skill.of("PostgreSQL", "postgres(?:ql)?", 2, true),
                Skill.of("MySQL", "mysql", 2, true),
                Skill.of("React", "react(?:\\.js|js)?", 3, true),
                Skill.of("JavaScript", "javascript|js", 2, true),
                Skill.of("HTML/CSS", "html5?|css3?", 1, true),
                Skill.of("Git", "git|github", 2, true),
                Skill.of("Maven", "maven", 1, true),
                Skill.of("JUnit/Mockito", "junit|mockito|unit test(?:ing|s)?", 2, true),
                Skill.of("Docker", "docker", 2, true),
                Skill.of("JWT", "jwt|oauth2?", 2, true),
                Skill.of("DSA", "data structures|algorithms|dsa", 2, true),
                Skill.of("OOP", "oops?|object[- ]oriented", 2, true),
                // ---- common asks NOT on the resume (have = false) ----
                Skill.of("Microservices", "micro-?services", 2, false),
                Skill.of("Kafka", "kafka", 2, false),
                Skill.of("AWS", "aws|amazon web services", 2, false),
                Skill.of("Azure/GCP", "azure|gcp|google cloud", 1, false),
                Skill.of("Kubernetes", "kubernetes|k8s", 2, false),
                Skill.of("Angular", "angular", 2, false),
                Skill.of("Node.js", "node(?:\\.js|js)", 2, false),
                Skill.of("Python", "python|django|flask", 3, false),
                Skill.of("Go", "golang", 2, false),
                Skill.of("C#/.NET", "\\.net|c#|asp\\.net", 3, false),
                Skill.of("PHP", "php|laravel", 3, false),
                Skill.of("MongoDB", "mongo(?:db)?", 1, false),
                Skill.of("Multithreading", "multithreading|concurrency", 1, false)
        );
    }
}
