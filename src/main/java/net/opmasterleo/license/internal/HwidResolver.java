package net.opmasterleo.license.internal;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.List;

public final class HwidResolver {

    private static final String[] ENV_CANDIDATES = {
            "OPLICENSE_HWID",
            "P_SERVER_UUID",            // Pterodactyl
            "P_SERVER_ID",              // Pterodactyl numeric id
            "SERVER_UUID",
            "PTERODACTYL_SERVER_UUID",
            "HOSTNAME"
    };

    private static final String[] SYSTEM_PROPERTY_CANDIDATES = {
            "oplicense.hwid",
            "server.uuid",
            "pterodactyl.server.uuid"
    };

    private HwidResolver() {
    }

    public static String resolveStable() {
        String override = firstNonBlank(systemProperties());
        if (override == null) {
            override = firstNonBlank(envValues());
        }
        if (override != null) {
            return fingerprint("OVERRIDE", override);
        }

        String machineId = readMachineId();
        if (machineId != null) {
            return fingerprint("MACHINE", machineId);
        }

        String mac = primaryMacAddress();
        if (mac != null) {
            return fingerprint("MAC", mac);
        }

        String legacy = System.getProperty("user.name", "unknown") + "-" + System.getProperty("os.name", "unknown");
        return fingerprint("LEGACY", legacy);
    }

    private static List<String> envValues() {
        List<String> values = new ArrayList<>();
        for (String key : ENV_CANDIDATES) {
            values.add(System.getenv(key));
        }
        return values;
    }

    private static List<String> systemProperties() {
        List<String> values = new ArrayList<>();
        for (String key : SYSTEM_PROPERTY_CANDIDATES) {
            values.add(System.getProperty(key));
        }
        return values;
    }

    private static String firstNonBlank(List<String> values) {
        for (String value : values) {
            if (value != null && !value.trim().isEmpty()) {
                return value.trim();
            }
        }
        return null;
    }

    private static String readMachineId() {
        String[] candidates = {
                "/etc/machine-id",
                "/var/lib/dbus/machine-id"
        };
        for (String candidate : candidates) {
            try {
                if (Files.exists(Path.of(candidate))) {
                    String value = Files.readString(Path.of(candidate), StandardCharsets.UTF_8).trim();
                    if (!value.isEmpty()) return value;
                }
            } catch (Exception ignored) {
            }
        }
        return null;
    }

    private static String primaryMacAddress() {
        try {
            NetworkInterface ni = NetworkInterface.getByInetAddress(InetAddress.getLocalHost());
            if (ni != null && ni.getHardwareAddress() != null) {
                StringBuilder sb = new StringBuilder();
                for (byte b : ni.getHardwareAddress()) {
                    sb.append(String.format("%02X", b));
                }
                return sb.toString();
            }
        } catch (Exception ignored) {
        }
        return null;
    }

    private static String fingerprint(String source, String raw) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest((source + ":" + raw).getBytes(StandardCharsets.UTF_8));
            StringBuilder out = new StringBuilder("HWID-");
            for (int i = 0; i < 12 && i < hash.length; i++) {
                out.append(String.format("%02X", hash[i]));
            }
            return out.toString();
        } catch (Exception e) {
            return "HWID-" + Integer.toHexString((source + ":" + raw).hashCode()).toUpperCase();
        }
    }
}
