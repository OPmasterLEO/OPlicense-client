package net.opmasterleo.license.internal.environment;

import net.opmasterleo.license.internal.platform.PlatformSupport;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.TimeUnit;

public final class EnvironmentResolver {

    private static final String[] PTERODACTYL_MARKERS = {
            "P_SERVER_UUID",
            "P_SERVER_ID",
            "PTERODACTYL_SERVER_UUID"
    };

    private static final String[] PTERODACTYL_NODE_KEYS = {
            "OPLICENSE_PTERODACTYL_NODE",
            "PTERODACTYL_NODE",
            "P_NODE_NAME",
            "P_NODE_ID",
            "NODE_NAME",
            "NODE_ID"
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
        if (value != null) {
            return value;
        }
        return isPterodactylLike() ? "?" : "unknown";
    }

    public static String resolveContainerLabel() {
        if (isPterodactylLike()) {
            return "pterodactyl";
        }
        return firstNonBlank(System.getenv("OPLICENSE_CONTAINER"), System.getProperty("oplicense.container"));
    }

    public static String resolveCpuModel() {
        String override = firstNonBlank(
                System.getenv("OPLICENSE_CPU_MODEL"),
                System.getProperty("oplicense.cpu.model")
        );
        if (override != null) {
            return override;
        }

        String linux = readLinuxCpuModel();
        if (linux != null) {
            return linux;
        }

        return readWindowsCpuModel();
    }

    public static double resolveCpuCores() {
        String override = firstNonBlank(
                System.getenv("OPLICENSE_CPU_CORES"),
                System.getProperty("oplicense.cpu.cores")
        );
        if (override != null) {
            double parsed = PlatformSupport.parseDouble(override, -1);
            if (parsed > 0) {
                return parsed;
            }
        }

        Double cgroupLimit = readCgroupCpuLimit();
        if (cgroupLimit != null && cgroupLimit > 0) {
            return cgroupLimit;
        }

        return Runtime.getRuntime().availableProcessors();
    }

    public static int resolveThreadCount() {
        return Runtime.getRuntime().availableProcessors();
    }

    public static String resolvePterodactylNode() {
        return firstNonBlank(envValues(PTERODACTYL_NODE_KEYS));
    }

    public static String resolvePterodactylServerId() {
        return firstNonBlank(
                System.getenv("OPLICENSE_PTERODACTYL_SERVER_ID"),
                System.getenv("P_SERVER_ID"),
                System.getenv("PTERODACTYL_SERVER_ID")
        );
    }

    public static String resolvePterodactylServerUuid() {
        return firstNonBlank(
                System.getenv("OPLICENSE_PTERODACTYL_SERVER_UUID"),
                System.getenv("P_SERVER_UUID"),
                System.getenv("SERVER_UUID"),
                System.getenv("PTERODACTYL_SERVER_UUID")
        );
    }

    public static boolean isPterodactylLike() {
        for (String key : PTERODACTYL_MARKERS) {
            String marker = System.getenv(key);
            if (marker != null && !marker.trim().isEmpty()) {
                return true;
            }
        }
        String container = System.getenv("container");
        return container != null && container.equalsIgnoreCase("pterodactyl");
    }

    private static Double readCgroupCpuLimit() {
        Double v2 = readCgroupV2CpuMax("/sys/fs/cgroup/cpu.max");
        if (v2 != null) {
            return v2;
        }

        v2 = readCgroupV2CpuMax("/sys/fs/cgroup/cpu/cpu.max");
        if (v2 != null) {
            return v2;
        }

        return readCgroupV1CpuLimit();
    }

    private static Double readCgroupV2CpuMax(String path) {
        String raw = PlatformSupport.readTextFile(path);
        if (raw == null || "max".equalsIgnoreCase(raw)) {
            return null;
        }

        String[] parts = raw.split("\\s+");
        if (parts.length != 2) {
            return null;
        }

        try {
            long quota = Long.parseLong(parts[0]);
            long period = Long.parseLong(parts[1]);
            if (quota <= 0 || period <= 0) {
                return null;
            }
            return (double) quota / period;
        } catch (Exception ignored) {
            return null;
        }
    }

    private static Double readCgroupV1CpuLimit() {
        String[][] candidates = {
                {
                        "/sys/fs/cgroup/cpu/cpu.cfs_quota_us",
                        "/sys/fs/cgroup/cpu/cpu.cfs_period_us"
                },
                {
                        "/sys/fs/cgroup/cpu,cpuacct/cpu.cfs_quota_us",
                        "/sys/fs/cgroup/cpu,cpuacct/cpu.cfs_period_us"
                }
        };

        for (String[] pair : candidates) {
            Double value = readCgroupV1Pair(pair[0], pair[1]);
            if (value != null) {
                return value;
            }
        }
        return null;
    }

    private static Double readCgroupV1Pair(String quotaPath, String periodPath) {
        try {
            if (!Files.exists(Path.of(quotaPath)) || !Files.exists(Path.of(periodPath))) {
                return null;
            }
            long quota = Long.parseLong(PlatformSupport.readTextFile(quotaPath));
            if (quota <= 0) {
                return null;
            }
            long period = Long.parseLong(PlatformSupport.readTextFile(periodPath));
            if (period <= 0) {
                return null;
            }
            return (double) quota / period;
        } catch (Exception ignored) {
            return null;
        }
    }

    private static String readLinuxCpuModel() {
        try {
            Path path = Path.of("/proc/cpuinfo");
            if (!Files.exists(path)) {
                return null;
            }
            List<String> lines = Files.readAllLines(path);
            for (int i = 0; i < lines.size(); i++) {
                String line = lines.get(i);
                String lower = line.toLowerCase();
                if (lower.startsWith("model name") || lower.startsWith("hardware")) {
                    int idx = line.indexOf(':');
                    if (idx >= 0) {
                        String value = line.substring(idx + 1).trim();
                        if (!value.isEmpty()) {
                            return value;
                        }
                    }
                }
            }
            return null;
        } catch (Exception ignored) {
            return null;
        }
    }

    private static String readWindowsCpuModel() {
        String os = System.getProperty("os.name", "");
        if (!os.toLowerCase().contains("win")) {
            return null;
        }

        Process process = null;
        try {
            process = new ProcessBuilder(
                    "powershell",
                    "-NoProfile",
                    "-Command",
                    "(Get-CimInstance Win32_Processor | Select-Object -First 1 -ExpandProperty Name)"
            ).redirectErrorStream(true).start();

            if (!process.waitFor(3, TimeUnit.SECONDS)) {
                process.destroyForcibly();
                return null;
            }
            if (process.exitValue() != 0) {
                return null;
            }

            InputStream input = process.getInputStream();
            byte[] bytes = input.readAllBytes();
            input.close();
            String output = new String(bytes, StandardCharsets.UTF_8).trim();
            if (output.isEmpty()) {
                return null;
            }
            return output;
        } catch (Exception ignored) {
            return null;
        } finally {
            if (process != null) {
                process.destroy();
            }
        }
    }

    private static String[] envValues(String[] keys) {
        String[] values = new String[keys.length];
        for (int i = 0; i < keys.length; i++) {
            values[i] = System.getenv(keys[i]);
        }
        return values;
    }

    private static String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.trim().isEmpty()) {
                return value.trim();
            }
        }
        return null;
    }
}
