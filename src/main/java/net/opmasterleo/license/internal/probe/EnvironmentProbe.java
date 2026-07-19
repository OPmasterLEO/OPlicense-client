package net.opmasterleo.license.internal.probe;

import net.opmasterleo.license.internal.util.Numbers;
import net.opmasterleo.license.internal.util.Strings;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.TimeUnit;

public final class EnvironmentProbe {

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

    private EnvironmentProbe() {
    }

    public static String resolveUserDir() {
        return Strings.firstNonBlank(
                System.getenv("OPLICENSE_USER_DIR"),
                System.getProperty("oplicense.user.dir"),
                System.getProperty("user.dir")
        );
    }

    public static String resolveUserHome() {
        return Strings.firstNonBlank(
                System.getenv("OPLICENSE_USER_HOME"),
                System.getProperty("oplicense.user.home"),
                System.getProperty("user.home")
        );
    }

    public static String resolveUserName() {
        String value = Strings.firstNonBlank(
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
        return Strings.firstNonBlank(System.getenv("OPLICENSE_CONTAINER"), System.getProperty("oplicense.container"));
    }

    public static String resolveCpuModel() {
        String override = Strings.firstNonBlank(
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
        String override = Strings.firstNonBlank(
                System.getenv("OPLICENSE_CPU_CORES"),
                System.getProperty("oplicense.cpu.cores")
        );
        if (override != null) {
            double parsed = Numbers.parseDouble(override, -1.0d);
            if (parsed > 0.0d) {
                return parsed;
            }
        }
        Double cgroupLimit = readCgroupCpuLimit();
        if (cgroupLimit != null && cgroupLimit.doubleValue() > 0.0d) {
            return cgroupLimit.doubleValue();
        }
        return Runtime.getRuntime().availableProcessors();
    }

    public static int resolveThreadCount() {
        return Runtime.getRuntime().availableProcessors();
    }

    public static String resolvePterodactylNode() {
        String[] values = new String[PTERODACTYL_NODE_KEYS.length];
        for (int i = 0; i < PTERODACTYL_NODE_KEYS.length; i++) {
            values[i] = System.getenv(PTERODACTYL_NODE_KEYS[i]);
        }
        return Strings.firstNonBlank(values);
    }

    public static String resolvePterodactylServerId() {
        return Strings.firstNonBlank(
                System.getenv("OPLICENSE_PTERODACTYL_SERVER_ID"),
                System.getenv("P_SERVER_ID"),
                System.getenv("PTERODACTYL_SERVER_ID")
        );
    }

    public static String resolvePterodactylServerUuid() {
        return Strings.firstNonBlank(
                System.getenv("OPLICENSE_PTERODACTYL_SERVER_UUID"),
                System.getenv("P_SERVER_UUID"),
                System.getenv("SERVER_UUID"),
                System.getenv("PTERODACTYL_SERVER_UUID")
        );
    }

    public static boolean isPterodactylLike() {
        for (int i = 0; i < PTERODACTYL_MARKERS.length; i++) {
            String marker = System.getenv(PTERODACTYL_MARKERS[i]);
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
        String raw = readTextFile(path);
        if (raw == null || "max".equalsIgnoreCase(raw)) {
            return null;
        }
        String[] parts = raw.split("\\s+");
        if (parts.length != 2) {
            return null;
        }
        Long quota = Numbers.parseLongOrNull(parts[0]);
        Long period = Numbers.parseLongOrNull(parts[1]);
        if (quota == null || period == null || quota.longValue() <= 0L || period.longValue() <= 0L) {
            return null;
        }
        return (double) quota.longValue() / (double) period.longValue();
    }

    private static Double readCgroupV1CpuLimit() {
        Double first = readCgroupV1Pair(
                "/sys/fs/cgroup/cpu/cpu.cfs_quota_us",
                "/sys/fs/cgroup/cpu/cpu.cfs_period_us"
        );
        if (first != null) {
            return first;
        }
        return readCgroupV1Pair(
                "/sys/fs/cgroup/cpu,cpuacct/cpu.cfs_quota_us",
                "/sys/fs/cgroup/cpu,cpuacct/cpu.cfs_period_us"
        );
    }

    private static Double readCgroupV1Pair(String quotaPath, String periodPath) {
        if (!Files.exists(Path.of(quotaPath)) || !Files.exists(Path.of(periodPath))) {
            return null;
        }
        Long quota = Numbers.parseLongOrNull(readTextFile(quotaPath));
        if (quota == null || quota.longValue() <= 0L) {
            return null;
        }
        Long period = Numbers.parseLongOrNull(readTextFile(periodPath));
        if (period == null || period.longValue() <= 0L) {
            return null;
        }
        return (double) quota.longValue() / (double) period.longValue();
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
                process.destroy();
                return null;
            }
            InputStream input = process.getInputStream();
            byte[] bytes = input.readAllBytes();
            input.close();
            process.destroy();
            String output = new String(bytes, StandardCharsets.UTF_8).trim();
            return output.isEmpty() ? null : output;
        } catch (Exception ignored) {
            if (process != null) {
                process.destroy();
            }
            return null;
        }
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
}
