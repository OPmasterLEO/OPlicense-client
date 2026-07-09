package net.opmasterleo.license.internal;

import java.util.ArrayList;
import java.util.List;

public final class EnvironmentResolver {

    private static final String[] PTERODACTYL_MARKERS = {
            "P_SERVER_UUID",
            "P_SERVER_ID",
            "PTERODACTYL_SERVER_UUID"
    };

    private EnvironmentResolver() {
    }

    public static String resolveUserDir() {
        return firstNonBlank(
                System.getenv("OPLICENSE_USER_DIR"),
                System.getProperty("oplicense.user.dir"),
                System.getProperty("user.dir")
        );
    }

    public static String resolveUserHome() {
        return firstNonBlank(
                System.getenv("OPLICENSE_USER_HOME"),
                System.getProperty("oplicense.user.home"),
                System.getProperty("user.home")
        );
    }

    public static String resolveUserName() {
        String value = firstNonBlank(
                System.getenv("OPLICENSE_USER_NAME"),
                System.getProperty("oplicense.user.name"),
                System.getenv("USER"),
                System.getenv("USERNAME"),
                System.getProperty("user.name")
        );
        if (value != null) return value;
        return isPterodactylLike() ? "?" : "unknown";
    }

    private static boolean isPterodactylLike() {
        for (String key : PTERODACTYL_MARKERS) {
            String marker = System.getenv(key);
            if (marker != null && !marker.trim().isEmpty()) {
                return true;
            }
        }
        String container = System.getenv("container");
        return container != null && container.equalsIgnoreCase("pterodactyl");
    }

    private static String firstNonBlank(String... values) {
        List<String> list = new ArrayList<>();
        for (String value : values) {
            list.add(value);
        }
        return firstNonBlank(list);
    }

    private static String firstNonBlank(List<String> values) {
        for (String value : values) {
            if (value != null && !value.trim().isEmpty()) {
                return value.trim();
            }
        }
        return null;
    }
}
