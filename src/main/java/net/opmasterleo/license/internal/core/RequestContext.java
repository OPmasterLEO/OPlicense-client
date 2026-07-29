package net.opmasterleo.license.internal.core;

import net.opmasterleo.license.internal.probe.EnvironmentProbe;
import net.opmasterleo.license.internal.probe.HardwareId;
import net.opmasterleo.license.model.LicenseEnvironment;

import java.util.LinkedHashMap;
import java.util.Map;

public final class RequestContext {

    private String hwid;
    private String macAddress;
    private String productVersion;
    private String operatingSystem;
    private String operatingSystemVersion;
    private String operatingSystemArchitecture;
    private String javaVersion;
    private String serverSoftware;
    private String serverSoftwareVersion;
    private String container;
    private String userDir;
    private String userHome;
    private String userName;
    private String pterodactylNode;
    private boolean bbb;
    private String bbbUserId;
    private String bbbUsername;
    private String bbbResourceId;
    private String bbbResourceTitle;
    private String bbbVersion;
    private String bbbVersionNumber;
    private String bbbDownloadTimestamp;
    private String bbbNonce;
    private String bbbSteam64;
    private String bbbSteam32;
    private boolean initialized;

    public RequestContext() {
    }

    private void ensureInitialized() {
        if (initialized) {
            return;
        }
        initialized = true;
        hwid = HardwareId.resolveStable();
        macAddress = HardwareId.resolvePrimaryMac();
        operatingSystem = System.getProperty("os.name");
        operatingSystemVersion = System.getProperty("os.version");
        operatingSystemArchitecture = System.getProperty("os.arch");
        javaVersion = System.getProperty("java.version");
        userDir = EnvironmentProbe.resolveUserDir();
        userHome = EnvironmentProbe.resolveUserHome();
        userName = EnvironmentProbe.resolveUserName();
    }

    public void setHwid(String hwid) {
        this.hwid = hwid;
    }

    public void useAutoHwid() {
        ensureInitialized();
        this.hwid = HardwareId.resolveStable();
    }

    public void setMacAddress(String macAddress) {
        this.macAddress = macAddress;
    }

    public void setProductVersion(String productVersion) {
        this.productVersion = productVersion;
    }

    public void setServerSoftware(String serverSoftware, String serverSoftwareVersion) {
        this.serverSoftware = serverSoftware;
        this.serverSoftwareVersion = serverSoftwareVersion;
    }

    public void setContainer(String container) {
        this.container = container;
    }

    public void setUserDir(String userDir) {
        this.userDir = userDir;
    }

    public void setUserHome(String userHome) {
        this.userHome = userHome;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public void setPterodactylNode(String pterodactylNode) {
        this.pterodactylNode = pterodactylNode;
    }

    public void setBuiltByBit(
            String userId,
            String username,
            String resourceId,
            String resourceTitle,
            String version,
            String versionNumber,
            String downloadTimestamp,
            String nonce
    ) {
        this.bbb = true;
        this.bbbUserId = userId;
        this.bbbUsername = username;
        this.bbbResourceId = resourceId;
        this.bbbResourceTitle = resourceTitle;
        this.bbbVersion = version;
        this.bbbVersionNumber = versionNumber;
        this.bbbDownloadTimestamp = downloadTimestamp;
        this.bbbNonce = nonce;
    }

    public void setBuiltByBitSteam(String steam64, String steam32) {
        this.bbbSteam64 = steam64;
        this.bbbSteam32 = steam32;
    }

    public void setBuiltByBitNonce(String nonce) {
        this.bbb = true;
        this.bbbNonce = nonce;
    }

    public String productVersion() {
        return productVersion;
    }

    public LicenseEnvironment environment() {
        ensureInitialized();
        return LicenseEnvironment.capture(userDir, userHome, userName, container, pterodactylNode);
    }

    public Map<String, Object> toRequestFields() {
        ensureInitialized();
        LicenseEnvironment environment = environment();
        Map<String, Object> fields = new LinkedHashMap<>();
        fields.put("timestamp", System.currentTimeMillis() / 1000L);
        fields.put("hwid", hwid);
        fields.put("macAddress", macAddress);
        fields.put("productVersion", productVersion);
        fields.put("operatingSystem", operatingSystem);
        fields.put("operatingSystemVersion", operatingSystemVersion);
        fields.put("operatingSystemArchitecture", operatingSystemArchitecture);
        fields.put("javaVersion", javaVersion);
        fields.put("serverSoftware", serverSoftware);
        fields.put("serverSoftwareVersion", serverSoftwareVersion);
        fields.put("container", environment.container());
        fields.put("userDir", environment.userDir());
        fields.put("userHome", environment.userHome());
        fields.put("userName", environment.userName());
        if (environment.cpuModel() != null) {
            fields.put("cpuModel", environment.cpuModel());
        }
        fields.put("cpuCores", environment.cpuCores());
        fields.put("threadCount", environment.threadCount());
        if (environment.pterodactylNode() != null) {
            fields.put("pterodactylNode", environment.pterodactylNode());
        }
        if (environment.pterodactylServerId() != null) {
            fields.put("pterodactylServerId", environment.pterodactylServerId());
        }
        if (environment.pterodactylServerUuid() != null) {
            fields.put("pterodactylServerUuid", environment.pterodactylServerUuid());
        }
        if (bbb) {
            fields.put("bbb", true);
        }
        putIfPresent(fields, "bbbUserId", bbbUserId);
        putIfPresent(fields, "bbbUsername", bbbUsername);
        putIfPresent(fields, "bbbResourceId", bbbResourceId);
        putIfPresent(fields, "bbbResourceTitle", bbbResourceTitle);
        putIfPresent(fields, "bbbVersion", bbbVersion);
        putIfPresent(fields, "bbbVersionNumber", bbbVersionNumber);
        putIfPresent(fields, "bbbDownloadTimestamp", bbbDownloadTimestamp);
        putIfPresent(fields, "bbbNonce", bbbNonce);
        putIfPresent(fields, "bbbSteam64", bbbSteam64);
        putIfPresent(fields, "bbbSteam32", bbbSteam32);
        return fields;
    }

    private static void putIfPresent(Map<String, Object> fields, String key, String value) {
        if (value != null && !value.isBlank()) {
            fields.put(key, value);
        }
    }
}
