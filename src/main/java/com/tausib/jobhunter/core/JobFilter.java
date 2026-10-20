package com.tausib.jobhunter.core;

import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Throws out postings that don't fit a 2027-batch student/fresher:
 * senior titles, 2+ years experience, and common scam signals (fees, deposits).
 */
public final class JobFilter {

    public record Result(boolean keep, String reason) {
        static Result accepted() { return new Result(true, null); }
        static Result drop(String why) { return new Result(false, why); }
    }

    // Whole words only: "lead" must not match "Leading", "sr" must not match "srinagar".
    private static final Pattern SENIOR_TITLE = Pattern.compile(
            "(?<![a-z])(senior|sr\\.?|lead|manager|principal|architect|staff engineer|head of|director|vp"
            + "|sde\\s*-?\\s*(?:2|ii|iii|3))(?![a-z])");

    // Whole words only: "intern" must not match "international"/"internal", and a bare
    // "graduate" ("graduate preferred") is not an entry-level signal.
    private static final Pattern ENTRY_LEVEL = Pattern.compile(
            "(?<![a-z])(interns?|internships?|freshers?|trainees?|entry[- ]level|new grads?|recent graduates?"
            + "|2027 batch|0\\s*-\\s*1 years?)(?![a-z])");

    private static final List<String> SCAM_WORDS = List.of(
            "registration fee", "training fee", "security deposit", "refundable deposit",
            "pay for the training", "course fee", "placement fee", "investment required");

    // "3+ years", "2-4 years", "5 yrs", "minimum 2 years"
    private static final Pattern YEARS = Pattern.compile("(\\d{1,2})\\s*\\+?\\s*(?:-|to|–)?\\s*\\d{0,2}\\s*\\+?\\s*(?:years|yrs|year)");

    private final int maxYearsExperience;
    private final List<String> locationMustContain;   // e.g. ["pune"]; empty = any location

    public JobFilter(int maxYearsExperience) {
        this(maxYearsExperience, List.of());
    }

    public JobFilter(int maxYearsExperience, List<String> locationMustContain) {
        this.maxYearsExperience = maxYearsExperience;
        this.locationMustContain = locationMustContain == null ? List.of()
                : locationMustContain.stream().map(JobFilter::lower).toList();
    }

    public Result check(JobListing job) {
        String title = lower(job.title());
        String desc = lower(job.description());

        if (!locationMustContain.isEmpty()) {
            String loc = lower(job.location());
            if (locationMustContain.stream().noneMatch(loc::contains)) {
                return Result.drop("location not wanted: '" + job.location() + "'");
            }
        }

        Matcher senior = SENIOR_TITLE.matcher(title);
        if (senior.find()) return Result.drop("senior title: '" + senior.group(1) + "'");
        for (String w : SCAM_WORDS) {
            if (desc.contains(w)) return Result.drop("red flag: '" + w + "'");
        }
        boolean entryLevel = isEntryLevel(title + " " + desc);
        int minYears = minYearsRequired(desc);
        if (!entryLevel && minYears > maxYearsExperience) {
            return Result.drop("asks " + minYears + "+ years experience");
        }
        return Result.accepted();
    }

    /** Smallest "N years" number mentioned near experience wording; 0 if none. */
    static int minYearsRequired(String desc) {
        if (desc == null || desc.isBlank()) return 0;
        Matcher m = YEARS.matcher(desc);
        int min = Integer.MAX_VALUE;
        while (m.find()) {
            int start = Math.max(0, m.start() - 60);
            int end = Math.min(desc.length(), m.end() + 40);
            String around = desc.substring(start, end);
            if (around.contains("experience") || around.contains("exp")) {
                min = Math.min(min, Integer.parseInt(m.group(1)));
            }
        }
        return min == Integer.MAX_VALUE ? 0 : min;
    }

    static boolean isEntryLevel(String text) {
        return ENTRY_LEVEL.matcher(text.toLowerCase(Locale.ROOT)).find();
    }

    private static String lower(String s) {
        return s == null ? "" : s.toLowerCase(Locale.ROOT);
    }
}
