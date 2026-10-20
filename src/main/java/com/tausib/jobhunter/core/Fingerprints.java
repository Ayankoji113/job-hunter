package com.tausib.jobhunter.core;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Locale;

/** De-duplication helpers: URL normalisation and a content fingerprint. */
public final class Fingerprints {

    private Fingerprints() {}

    /**
     * Strips tracking query params, fragments and trailing slashes so the same job
     * shared with different ?trk=... links is stored once.
     * Example: https://in.linkedin.com/jobs/view/123/?trk=abc -> https://linkedin.com/jobs/view/123
     */
    public static String normalizeUrl(String raw) {
        if (raw == null || raw.isBlank()) return null;
        try {
            URI uri = URI.create(raw.trim());
            String host = uri.getHost() == null ? "" : uri.getHost().toLowerCase(Locale.ROOT);
            // in.linkedin.com, www.linkedin.com -> linkedin.com
            host = host.replaceFirst("^(www\\.|[a-z]{2}\\.)(?=linkedin\\.com$)", "");
            host = host.replaceFirst("^www\\.", "");
            String path = uri.getPath() == null ? "" : uri.getPath();
            while (path.endsWith("/") && path.length() > 1) path = path.substring(0, path.length() - 1);
            return "https://" + host + path;
        } catch (IllegalArgumentException e) {
            return raw.trim();
        }
    }

    public static String of(String company, String title, String location) {
        String key = clean(company) + "|" + clean(title) + "|" + clean(location);
        try {
            byte[] hash = MessageDigest.getInstance("SHA-256").digest(key.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash, 0, 16);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    private static String clean(String s) {
        return s == null ? "" : s.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+", " ").trim();
    }
}
