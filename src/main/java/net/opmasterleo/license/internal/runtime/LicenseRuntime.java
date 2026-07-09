package net.opmasterleo.license.internal.runtime;

import net.opmasterleo.license.internal.environment.EnvironmentResolver;
import net.opmasterleo.license.internal.hardware.HwidResolver;
import net.opmasterleo.license.model.LicenseEnvironment;

import java.util.LinkedHashMap;
import java.util.Map;

/** Mutable runtime metadata sent with validation requests. */
public final class LicenseRuntime {

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
    private boolean initialized;

    public LicenseRuntime() {
    }

    private void ensureInitialized() {
        if (initialized) {
            return;
        }
        initialized = true;
        hwid = HwidResolver.resolveStable();
        operatingSystem = System.getProperty("os.name");
        operatingSystemVersion = System.getProperty("os.version");
        operatingSystemArchitecture = System.getProperty("os.arch");
        javaVersion = System.getProperty("java.version");
        userDir = EnvironmentResolver.resolveUserDir();
        userHome = EnvironmentResolver.resolveUserHome();
        userName = EnvironmentResolver.resolveUserName();
    }

    public LicenseRuntime setHwid(String hwid) {
        this.hwid = hwid;
        return this;
    }

    public LicenseRuntime useAutoHwid() {
        ensureInitialized();
        this.hwid = HwidResolver.resolveStable();
        return this;
    }

    public LicenseRuntime setMacAddress(String macAddress) {
        this.macAddress = macAddress;
        return this;
    }

    public LicenseRuntime setProductVersion(String productVersion) {
        this.productVersion = productVersion;
        return this;
    }

    public LicenseRuntime setServerSoftware(String serverSoftware, String serverSoftwareVersion) {
        this.serverSoftware = serverSoftware;
        this.serverSoftwareVersion = serverSoftwareVersion;
        return this;
    }

    public LicenseRuntime setContainer(String container) {
        this.container = container;
        return this;
    }

    public LicenseRuntime setUserDir(String userDir) {
        this.userDir = userDir;
        return this;
    }

    public LicenseRuntime setUserHome(String userHome) {
        this.userHome = userHome;
        return this;
    }

    public LicenseRuntime setUserName(String userName) {
        this.userName = userName;
        return this;
    }

    public LicenseRuntime setPterodactylNode(String pterodactylNode) {
        this.pterodactylNode = pterodactylNode;
        return this;
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
        fields.put("timestamp", System.currentTimeMillis() / 1000);
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
        return fields;
    }
}
