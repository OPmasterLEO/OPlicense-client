package net.opmasterleo.license.internal.platform;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

public final class PlatformSupport {

    @FunctionalInterface
    public interface ThrowingSupplier<T> {
        T get() throws Exception;
    }

    @FunctionalInterface
    public interface ThrowingRunnable {
        void run() throws Exception;
    }

    private PlatformSupport() {
    }

    public static <T> T call(ThrowingSupplier<T> supplier) {
        return call(supplier, null);
    }

    public static <T> T call(ThrowingSupplier<T> supplier, T fallback) {
        try {
            return supplier.get();
        } catch (Exception ignored) {
            return fallback;
        }
    }

    public static void run(ThrowingRunnable runnable) {
        call(() -> {
            runnable.run();
            return null;
        });
    }

    public static String readTextFile(String path) {
        return call(() -> {
            Path file = Path.of(path);
            if (!Files.exists(file)) {
                return null;
            }
            String content = Files.readString(file, StandardCharsets.UTF_8).trim();
            return content.isEmpty() ? null : content;
        });
    }

    public static double parseDouble(String value, double fallback) {
        if (value == null || value.isBlank()) {
            return fallback;
        }
        return call(() -> Double.parseDouble(value.trim()), fallback);
    }

    public static void sleep(long millis) throws InterruptedException {
        Thread.sleep(millis);
    }
}
