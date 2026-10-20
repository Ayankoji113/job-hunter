package com.tausib.jobhunter.core;

import java.util.List;
import java.util.Map;

/**
 * Converts one raw dataset item (a JSON object as a Map) into a {@link JobListing}.
 *
 * Different Apify actors name their fields differently ("title" vs "jobTitle",
 * "companyName" vs "company.name"). Instead of hard-coding one actor, we try a list
 * of candidate keys in order. Dotted keys ("company.name") walk into nested objects.
 */
public final class ApifyItemMapper {

    private static final List<String> TITLE = List.of("title", "jobTitle", "position", "name");
    private static final List<String> COMPANY = List.of("companyName", "company.name", "company", "employer", "organization");
    // LinkedIn (harvestapi), Internshala (blackfalcondata) and Naukri (valig) field names are all covered.
    private static final List<String> LOCATION = List.of("location.linkedinText", "location", "locations.0.label", "jobLocation", "formattedLocation", "city");
    private static final List<String> URL = List.of("linkedinUrl", "jobUrl", "url", "canonicalUrl", "link", "jobLink", "sourceUrl", "externalUrl");
    private static final List<String> APPLY = List.of("applyUrl", "applyMethod.companyApplyUrl", "applicationUrl", "externalApplyLink");
    private static final List<String> DESC = List.of("descriptionText", "description.full", "description", "jobDescription", "descriptionHtml", "snippet");
    private static final List<String> POSTED = List.of("postedDate", "postedAt", "createdDateText", "publishedAt", "listedAt", "datePosted", "postedTimeAgo");
    private static final List<String> ID = List.of("id", "jobId", "jobPostingId");

    private ApifyItemMapper() {}

    /** @return the listing, or null if the item has no title or no URL (useless to us). */
    public static JobListing map(Map<String, Object> item, String source, String searchKeyword) {
        String title = first(item, TITLE);
        String url = Fingerprints.normalizeUrl(first(item, URL));
        if (title == null || url == null) return null;
        String desc = first(item, DESC);
        if (desc != null) desc = desc.replaceAll("<[^>]+>", " ").replaceAll("\\s+", " ").trim();
        String skills = skillsText(item.get("skills"));      // Internshala list / Naukri {preferred, other}
        if (!skills.isEmpty()) desc = (desc == null ? "" : desc + " ") + "Skills: " + skills;
        String location = first(item, LOCATION);
        if (isRemote(item) && (location == null || !location.toLowerCase().matches(".*(remote|home).*"))) {
            location = (location == null ? "" : location + " ") + "(Remote)";
        }
        return new JobListing(
                source,
                first(item, ID),
                title.trim(),
                first(item, COMPANY),
                location,
                url,
                first(item, APPLY),
                desc,
                first(item, POSTED),
                searchKeyword);
    }

    static String first(Map<String, Object> item, List<String> keys) {
        for (String key : keys) {
            Object v = dig(item, key);
            if (v instanceof String s && !s.isBlank()) return s;
            if (v instanceof Number n) return n.toString();
        }
        return null;
    }

    /** LinkedIn: workplaceType "remote" / workRemoteAllowed; Internshala: isWorkFromHome / workMode "wfh". */
    static boolean isRemote(Map<String, Object> item) {
        return Boolean.TRUE.equals(item.get("workRemoteAllowed"))
                || Boolean.TRUE.equals(item.get("isWorkFromHome"))
                || "remote".equalsIgnoreCase(String.valueOf(item.get("workplaceType")))
                || "wfh".equalsIgnoreCase(String.valueOf(item.get("workMode")));
    }

    /** Flattens a skills list, or a map of lists, into "Java, Spring Boot, ...". */
    static String skillsText(Object skills) {
        List<String> out = new java.util.ArrayList<>();
        collect(skills, out);
        return String.join(", ", out);
    }

    private static void collect(Object o, List<String> out) {
        if (o instanceof String s && !s.isBlank()) out.add(s.trim());
        else if (o instanceof List<?> l) l.forEach(x -> collect(x, out));
        else if (o instanceof Map<?, ?> m) m.values().forEach(x -> collect(x, out));
    }

    /** Walks "a.b.c"; a numeric part indexes into a list ("locations.0.label"). */
    @SuppressWarnings("unchecked")
    private static Object dig(Map<String, Object> item, String dottedKey) {
        Object cur = item;
        for (String part : dottedKey.split("\\.")) {
            if (cur instanceof Map<?, ?> m) {
                cur = ((Map<String, Object>) m).get(part);
            } else if (cur instanceof List<?> l && part.chars().allMatch(Character::isDigit)) {
                int i = Integer.parseInt(part);
                cur = i < l.size() ? l.get(i) : null;
            } else {
                return null;
            }
        }
        return cur;
    }
}
