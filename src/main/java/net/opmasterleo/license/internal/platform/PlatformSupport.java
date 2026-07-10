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
        if (value == null || value.isBlank()) {
            return fallback;
        }
        try {
            return Double.parseDouble(value.trim());
        } catch (Exception ignored) {
            return fallback;
        }
    }

    public static void sleep(long millis) throws InterruptedException {
        Thread.sleep(millis);
    }
}
