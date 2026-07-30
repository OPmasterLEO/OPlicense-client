package net.opmasterleo.license.internal.core;

import net.opmasterleo.license.internal.probe.EnvironmentProbe;
import net.opmasterleo.license.internal.probe.HardwareId;
import net.opmasterleo.license.model.LicenseEnvironment;

import java.util.LinkedHashMap;
import java.util.Map;

public final class RequestContext {

    private static final String BBB_BUILTBYBIT = "%%__BUILTBYBIT__%%";
    private static final String BBB_USER = "%%__USER__%%";
    private static final String BBB_USERNAME = "%%__USERNAME__%%";
    private static final String BBB_RESOURCE = "%%__RESOURCE__%%";
    private static final String BBB_RESOURCE_TITLE = "%%__RESOURCE_TITLE__%%";
    private static final String BBB_VERSION = "%%__VERSION__%%";
    private static final String BBB_VERSION_NUMBER = "%%__VERSION_NUMBER__%%";
    private static final String BBB_TIMESTAMP = "%%__TIMESTAMP__%%";
    private static final String BBB_NONCE = "%%__NONCE__%%";
    private static final String BBB_STEAM64 = "%%__STEAM64__%%";
    private static final String BBB_STEAM32 = "%%__STEAM32__%%";

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
    private String bbb = BBB_BUILTBYBIT;
    private String bbbUserId = BBB_USER;
    private String bbbUsername = BBB_USERNAME;
    private String bbbResourceId = BBB_RESOURCE;
    private String bbbResourceTitle = BBB_RESOURCE_TITLE;
    private String bbbVersion = BBB_VERSION;
    private String bbbVersionNumber = BBB_VERSION_NUMBER;
    private String bbbDownloadTimestamp = BBB_TIMESTAMP;
    private String bbbNonce = BBB_NONCE;
    private String bbbSteam64 = BBB_STEAM64;
    private String bbbSteam32 = BBB_STEAM32;
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
        this.bbb = BBB_BUILTBYBIT;
        this.bbbUserId = orPlaceholder(userId, BBB_USER);
        this.bbbUsername = orPlaceholder(username, BBB_USERNAME);
        this.bbbResourceId = orPlaceholder(resourceId, BBB_RESOURCE);
        this.bbbResourceTitle = orPlaceholder(resourceTitle, BBB_RESOURCE_TITLE);
        this.bbbVersion = orPlaceholder(version, BBB_VERSION);
        this.bbbVersionNumber = orPlaceholder(versionNumber, BBB_VERSION_NUMBER);
        this.bbbDownloadTimestamp = orPlaceholder(downloadTimestamp, BBB_TIMESTAMP);
        this.bbbNonce = orPlaceholder(nonce, BBB_NONCE);
    }

    public void setBuiltByBitSteam(String steam64, String steam32) {
        this.bbbSteam64 = orPlaceholder(steam64, BBB_STEAM64);
        this.bbbSteam32 = orPlaceholder(steam32, BBB_STEAM32);
    }

    public void setBuiltByBitNonce(String nonce) {
        this.bbbNonce = orPlaceholder(nonce, BBB_NONCE);
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
        fields.put("bbb", bbb);
        fields.put("bbbUserId", bbbUserId);
        fields.put("bbbUsername", bbbUsername);
        fields.put("bbbResourceId", bbbResourceId);
        fields.put("bbbResourceTitle", bbbResourceTitle);
        fields.put("bbbVersion", bbbVersion);
        fields.put("bbbVersionNumber", bbbVersionNumber);
        fields.put("bbbDownloadTimestamp", bbbDownloadTimestamp);
        fields.put("bbbNonce", bbbNonce);
        fields.put("bbbSteam64", bbbSteam64);
        fields.put("bbbSteam32", bbbSteam32);
        return fields;
    }

    private static String orPlaceholder(String value, String placeholder) {
        if (value == null || value.isBlank()) {
            return placeholder;
        }
        return value;
    }
}
