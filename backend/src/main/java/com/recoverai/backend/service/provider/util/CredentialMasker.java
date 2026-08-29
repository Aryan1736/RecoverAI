package com.recoverai.backend.service.provider.util;

import java.util.Locale;
import java.util.regex.Pattern;

public final class CredentialMasker {

    private static final Pattern CARD_PATTERN = Pattern.compile("\\b(?:\\d[ -]*?){13,19}\\b");
    private static final Pattern AUTH_HEADER_PATTERN = Pattern.compile("(?i)(authorization\\s*:\\s*(?:bearer\\s+|basic\\s+)?|bearer\\s+|(?:token|api[-_]?key|secret|password)\\s*[:=]\\s*)([\"']?[^\"',;\\s]+[\"']?)");

    private CredentialMasker() {
    }

    public static String mask(String secret) {
        if (secret == null || secret.isBlank()) {
            return "[EMPTY]";
        }
        int length = secret.length();
        if (length <= 4) {
            return "****";
        }
        if (length <= 8) {
            return secret.substring(0, 1) + "****" + secret.substring(length - 1);
        }
        return secret.substring(0, 3) + "..." + secret.substring(length - 3);
    }

    public static String maskPhone(String phone) {
        if (phone == null || phone.isBlank()) {
            return "ANONYMOUS";
        }
        String clean = phone.trim();
        if (clean.length() <= 4) {
            return "****";
        }
        return clean.substring(0, Math.min(3, clean.length())) + "****" + clean.substring(clean.length() - 2);
    }

    public static String maskEmail(String email) {
        if (email == null || !email.contains("@")) {
            return "ANONYMOUS";
        }
        int atIndex = email.indexOf('@');
        if (atIndex <= 1) {
            return "*@" + email.substring(atIndex + 1);
        }
        return email.charAt(0) + "***@" + email.substring(atIndex + 1);
    }

    public static String sanitizeMessage(String message) {
        if (message == null) {
            return null;
        }
        String sanitized = AUTH_HEADER_PATTERN.matcher(message).replaceAll("$1[PROTECTED]");
        sanitized = CARD_PATTERN.matcher(sanitized).replaceAll("[PROTECTED_CARD]");
        return sanitized;
    }
}
