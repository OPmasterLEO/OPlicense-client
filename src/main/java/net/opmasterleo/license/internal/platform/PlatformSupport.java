package net.opmasterleo.license.internal.platform;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

public final class PlatformSupport {

    private PlatformSupport() {
    }

    public static String readTextFile(String path) {
        try {
            Path file = Path.of(path);
            if (!Files.exists(file)) {
                return null;
            }
            String content = Files.readString(file, StandardCharsets.UTF_8).trim();
            if (content.isEmpty()) {
                return null;
            }
            return content;
        } catch (Exception ignored) {
            return null;
        }
    }

    public static double parseDouble(String value, double fallback) {
        if (value == null || value.isEmpty()) {
            return fallback;
        }
        String trimmed = value.trim();
        if (trimmed.isEmpty()) {
            return fallback;
        }

        boolean negative = false;
        int index = 0;
        if (trimmed.charAt(0) == '-') {
            negative = true;
            index = 1;
        }
        if (index >= trimmed.length()) {
            return fallback;
        }

        long whole = 0;
        long fraction = 0;
        long fractionDivisor = 1;
        boolean inFraction = false;

        for (int i = index; i < trimmed.length(); i++) {
            char ch = trimmed.charAt(i);
            if (ch == '.') {
                if (inFraction) {
                    return fallback;
                }
                inFraction = true;
                continue;
            }
            if (ch < '0' || ch > '9') {
                return fallback;
            }
            int digit = ch - '0';
            if (!inFraction) {
                whole = (whole * 10L) + digit;
            } else {
                fraction = (fraction * 10L) + digit;
                fractionDivisor *= 10L;
            }
        }

        double result = whole + ((double) fraction / (double) fractionDivisor);
        if (negative) {
            result = -result;
        }
        return result;
    }

    public static Long parseLongOrNull(String value) {
        if (value == null || value.isEmpty()) {
            return null;
        }
        int index = 0;
        boolean negative = false;
        if (value.charAt(0) == '-') {
            negative = true;
            index = 1;
        }
        if (index >= value.length()) {
            return null;
        }
        long result = 0;
        for (int i = index; i < value.length(); i++) {
            char digit = value.charAt(i);
            if (digit < '0' || digit > '9') {
                return null;
            }
            result = (result * 10L) + (digit - '0');
        }
        if (negative) {
            return -result;
        }
        return result;
    }

    public static void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (Exception ignored) {
        }
    }
}
