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
        return fields;
    }
}
