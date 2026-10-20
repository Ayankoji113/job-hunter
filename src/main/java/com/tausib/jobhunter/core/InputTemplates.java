package com.tausib.jobhunter.core;

/**
 * Fills the actor input template from application.yml.
 *
 * Template example:  {"keyword": "{keyword}", "location": "{location}", "limit": {limit}}
 * {keyword} and {location} are JSON-escaped (safe inside quotes); {limit} is a bare number.
 */
public final class InputTemplates {

    private InputTemplates() {}

    public static String render(String template, String keyword, String location, int limit) {
        return template
                .replace("{keyword}", escape(keyword))
                .replace("{location}", escape(location))
                .replace("{limit}", Integer.toString(limit));
    }

    static String escape(String s) {
        if (s == null) return "";
        StringBuilder sb = new StringBuilder(s.length() + 8);
        for (char c : s.toCharArray()) {
            switch (c) {
                case '"' -> sb.append("\\\"");
                case '\\' -> sb.append("\\\\");
                case '\n' -> sb.append("\\n");
                case '\r' -> sb.append("\\r");
                case '\t' -> sb.append("\\t");
                default -> {
                    if (c < 0x20) sb.append(String.format("\\u%04x", (int) c));
                    else sb.append(c);
                }
            }
        }
        return sb.toString();
    }
}
