package com.sentinellesms.util;

import java.net.URI;
import java.util.Locale;
import java.util.regex.Pattern;

public final class NormalizationUtils {

    private static final Pattern NON_DIGIT = Pattern.compile("\\D+");
    private static final Pattern MALI_LOCAL = Pattern.compile("^\\d{8}$");

    private NormalizationUtils() {
    }

    /**
     * Normalise un numéro malien vers un format E.164 simplifié (+223XXXXXXXX).
     */
    public static String normalizePhone(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        String digits = NON_DIGIT.matcher(raw.trim()).replaceAll("");
        if (digits.startsWith("00223")) {
            digits = digits.substring(2);
        }
        if (digits.startsWith("223") && digits.length() == 11) {
            return "+" + digits;
        }
        if (MALI_LOCAL.matcher(digits).matches()) {
            return "+223" + digits;
        }
        if (digits.startsWith("0") && digits.length() == 9) {
            return "+223" + digits.substring(1);
        }
        return digits.isEmpty() ? null : "+" + digits;
    }

    public static String normalizeUrl(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        String value = raw.trim();
        if (!value.matches("(?i)^https?://.*")) {
            value = "https://" + value;
        }
        try {
            URI uri = URI.create(value);
            String host = uri.getHost() == null ? "" : uri.getHost().toLowerCase(Locale.ROOT);
            String path = uri.getPath() == null || pathIsRoot(uri.getPath()) ? "" : uri.getPath();
            String query = uri.getQuery() == null ? "" : "?" + uri.getQuery();
            return ("https://" + host + path + query).toLowerCase(Locale.ROOT);
        } catch (IllegalArgumentException ex) {
            return value.toLowerCase(Locale.ROOT);
        }
    }

    public static String extractDomain(String rawUrl) {
        String normalized = normalizeUrl(rawUrl);
        if (normalized == null) {
            return null;
        }
        try {
            URI uri = URI.create(normalized);
            return uri.getHost() == null ? null : uri.getHost().toLowerCase(Locale.ROOT);
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

    private static boolean pathIsRoot(String path) {
        return path == null || path.isBlank() || "/".equals(path);
    }
}
