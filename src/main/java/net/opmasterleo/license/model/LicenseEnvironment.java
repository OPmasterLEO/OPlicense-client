package net.opmasterleo.license.model;

import net.opmasterleo.license.internal.probe.EnvironmentProbe;

/** Captured server environment metadata. */
public final class LicenseEnvironment {

    private final String userDir;
    private final String userHome;
    private final String userName;
    private final String container;
    private final String cpuModel;
    private final double cpuCores;
    private final int threadCount;
    private final String pterodactylNode;
    private final String pterodactylServerId;
    private final String pterodactylServerUuid;

    /** Creates an environment snapshot. */
    public LicenseEnvironment(
            String userDir,
            String userHome,
            String userName,
            String container,
            String cpuModel,
            double cpuCores,
            int threadCount,
            String pterodactylNode,
            String pterodactylServerId,
            String pterodactylServerUuid
    ) {
        this.userDir = userDir;
        this.userHome = userHome;
        this.userName = userName;
        this.container = container;
        this.cpuModel = cpuModel;
        this.cpuCores = cpuCores;
        this.threadCount = threadCount;
        this.pterodactylNode = pterodactylNode;
        this.pterodactylServerId = pterodactylServerId;
        this.pterodactylServerUuid = pterodactylServerUuid;
    }

    /** Captures environment details with automatic CPU/container detection. */
    public static LicenseEnvironment capture(
            String userDir,
            String userHome,
            String userName,
            String container,
            String pterodactylNode
    ) {
        return new LicenseEnvironment(
                userDir,
                userHome,
                userName,
                container != null ? container : EnvironmentProbe.resolveContainerLabel(),
                EnvironmentProbe.resolveCpuModel(),
                EnvironmentProbe.resolveCpuCores(),
                EnvironmentProbe.resolveThreadCount(),
                pterodactylNode != null ? pterodactylNode : EnvironmentProbe.resolvePterodactylNode(),
                EnvironmentProbe.resolvePterodactylServerId(),
                EnvironmentProbe.resolvePterodactylServerUuid()
        );
    }

    /** Returns the working directory. */
    public String userDir() {
        return userDir;
    }

    /** Returns the user home directory. */
    public String userHome() {
        return userHome;
    }

    /** Returns the username. */
    public String userName() {
        return userName;
    }

    /** Returns the container label. */
    public String container() {
        return container;
    }

    /** Returns the CPU model. */
    public String cpuModel() {
        return cpuModel;
    }

    /** Returns allocated CPU cores. */
    public double cpuCores() {
        return cpuCores;
    }

    /** Returns the logical thread count. */
    public int threadCount() {
        return threadCount;
    }

    /** Returns the Pterodactyl node name. */
    public String pterodactylNode() {
        return pterodactylNode;
    }

    /** Returns the Pterodactyl server id. */
    public String pterodactylServerId() {
        return pterodactylServerId;
    }

    /** Returns the Pterodactyl server UUID. */
    public String pterodactylServerUuid() {
        return pterodactylServerUuid;
    }
}
