package net.opmasterleo.license.internal.util;

public final class Numbers {

    private Numbers() {
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
        long result = 0L;
        for (int i = index; i < value.length(); i++) {
            char digit = value.charAt(i);
            if (digit < '0' || digit > '9') {
                return null;
            }
            result = (result * 10L) + (long) (digit - '0');
        }
        return negative ? -result : result;
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

        long whole = 0L;
        long fraction = 0L;
        long fractionDivisor = 1L;
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
                whole = (whole * 10L) + (long) digit;
            } else {
                fraction = (fraction * 10L) + (long) digit;
                fractionDivisor *= 10L;
            }
        }

        double result = (double) whole + ((double) fraction / (double) fractionDivisor);
        return negative ? -result : result;
    }
}
