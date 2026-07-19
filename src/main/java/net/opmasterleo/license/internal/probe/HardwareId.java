package net.opmasterleo.license.internal.probe;

import net.opmasterleo.license.internal.util.Digests;

import java.net.NetworkInterface;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Enumeration;

public final class HardwareId {

    private static final String[] ENV_KEYS = {
            "OPLICENSE_HWID",
            "P_SERVER_UUID",
            "P_SERVER_ID",
            "SERVER_UUID",
            "PTERODACTYL_SERVER_UUID",
            "HOSTNAME"
    };

    private static final String[] PROPERTY_KEYS = {
            "oplicense.hwid",
            "server.uuid",
            "pterodactyl.server.uuid"
    };

    private HardwareId() {
    }

    public static String resolveStable() {
        String override = firstPresent(PROPERTY_KEYS, true);
        if (override == null) {
            override = firstPresent(ENV_KEYS, false);
        }
        if (override != null) {
            return fingerprint("OVERRIDE", override);
        }

        String machineId = readTextFile("/etc/machine-id");
        if (machineId == null) {
            machineId = readTextFile("/var/lib/dbus/machine-id");
        }
        if (machineId != null) {
            return fingerprint("MACHINE", machineId);
        }

        String mac = resolvePrimaryMac();
        if (mac != null) {
            return fingerprint("MAC", mac);
        }

        String legacy = System.getProperty("user.name", "unknown")
                + "-"
                + System.getProperty("os.name", "unknown");
        return fingerprint("LEGACY", legacy);
    }

    public static String resolvePrimaryMac() {
        return firstHardwareMac();
    }

    private static String firstPresent(String[] keys, boolean systemProperty) {
        for (int i = 0; i < keys.length; i++) {
            String value;
            if (systemProperty) {
                value = System.getProperty(keys[i]);
            } else {
                value = System.getenv(keys[i]);
            }
            if (value != null && !value.trim().isEmpty()) {
                return value.trim();
            }
        }
        return null;
    }

    private static String fingerprint(String source, String raw) {
        return "HWID-" + Digests.sha256HexPrefix(source + ":" + raw, 12);
    }

    private static String readTextFile(String path) {
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

    private static String firstHardwareMac() {
        try {
            Enumeration<NetworkInterface> networks = NetworkInterface.getNetworkInterfaces();
            while (networks.hasMoreElements()) {
                NetworkInterface network = networks.nextElement();
                byte[] hardwareAddress = network.getHardwareAddress();
                if (hardwareAddress == null || hardwareAddress.length == 0) {
                    continue;
                }
                StringBuilder builder = new StringBuilder();
                for (int i = 0; i < hardwareAddress.length; i++) {
                    builder.append(String.format("%02X", hardwareAddress[i] & 0xff));
                    if (i + 1 < hardwareAddress.length) {
                        builder.append(':');
                    }
                }
                if (builder.length() > 0) {
                    return builder.toString();
                }
            }
            return null;
        } catch (Exception ignored) {
            return null;
        }
    }
}
