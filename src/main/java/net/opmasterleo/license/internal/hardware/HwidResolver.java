package net.opmasterleo.license.internal.hardware;

import net.opmasterleo.license.internal.platform.PlatformSupport;

import java.net.NetworkInterface;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Enumeration;

public final class HwidResolver {

    private static final String[] ENV_CANDIDATES = {
            "OPLICENSE_HWID",
            "P_SERVER_UUID",
            "P_SERVER_ID",
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

    public static String resolvePrimaryMac() {
        return primaryMacAddress();
    }

    private static String[] envValues() {
        String[] values = new String[ENV_CANDIDATES.length];
        for (int i = 0; i < ENV_CANDIDATES.length; i++) {
            values[i] = System.getenv(ENV_CANDIDATES[i]);
        }
        return values;
    }

    private static String[] systemProperties() {
        String[] values = new String[SYSTEM_PROPERTY_CANDIDATES.length];
        for (int i = 0; i < SYSTEM_PROPERTY_CANDIDATES.length; i++) {
            values[i] = System.getProperty(SYSTEM_PROPERTY_CANDIDATES[i]);
        }
        return values;
    }

    private static String firstNonBlank(String... values) {
        for (int i = 0; i < values.length; i++) {
            String value = values[i];
            if (value != null && !value.trim().isEmpty()) {
                return value.trim();
            }
        }
        return null;
    }

    private static String readMachineId() {
        String machineId = PlatformSupport.readTextFile("/etc/machine-id");
        if (machineId != null) {
            return machineId;
        }
        return PlatformSupport.readTextFile("/var/lib/dbus/machine-id");
    }

    private static String primaryMacAddress() {
        try {
            Enumeration<NetworkInterface> networks = NetworkInterface.getNetworkInterfaces();
            while (networks.hasMoreElements()) {
                NetworkInterface network = networks.nextElement();
                byte[] hardwareAddress = network.getHardwareAddress();
                if (hardwareAddress == null) {
                    continue;
                }

                StringBuilder builder = new StringBuilder();
                for (int i = 0; i < hardwareAddress.length; i++) {
                    builder.append(String.format("%02X", hardwareAddress[i]));
                    if (i + 1 < hardwareAddress.length) {
                        builder.append(':');
                    }
                }
                if (builder.length() > 0) {
                    return builder.toString();
                }
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
            for (int i = 0; i < hash.length && i < 12; i++) {
                out.append(String.format("%02X", hash[i]));
            }
            return out.toString();
        } catch (Exception ignored) {
            return "HWID-" + Integer.toHexString((source + ":" + raw).hashCode()).toUpperCase();
        }
    }
}
