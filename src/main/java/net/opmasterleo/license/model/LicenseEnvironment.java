package net.opmasterleo.license.model;

import net.opmasterleo.license.internal.environment.EnvironmentResolver;

public final class LicenseEnvironment {

    private final String userDir;
    private final String userHome;
    private final String userName;
    private final String container;
    private final double cpuCores;
    private final int threadCount;
    private final String pterodactylNode;
    private final String pterodactylServerId;
    private final String pterodactylServerUuid;

    public LicenseEnvironment(
            String userDir,
            String userHome,
            String userName,
            String container,
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
        this.cpuCores = cpuCores;
        this.threadCount = threadCount;
        this.pterodactylNode = pterodactylNode;
        this.pterodactylServerId = pterodactylServerId;
        this.pterodactylServerUuid = pterodactylServerUuid;
    }

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
                container != null ? container : EnvironmentResolver.resolveContainerLabel(),
                EnvironmentResolver.resolveCpuCores(),
                EnvironmentResolver.resolveThreadCount(),
                pterodactylNode != null ? pterodactylNode : EnvironmentResolver.resolvePterodactylNode(),
                EnvironmentResolver.resolvePterodactylServerId(),
                EnvironmentResolver.resolvePterodactylServerUuid()
        );
    }

    public String userDir() {
        return userDir;
    }

    public String userHome() {
        return userHome;
    }

    public String userName() {
        return userName;
    }

    public String container() {
        return container;
    }

    /** CPU cores allocated to this JVM process (cgroup-aware on Pterodactyl/Docker). */
    public double cpuCores() {
        return cpuCores;
    }

    /** Live JVM thread count for this server process (not the whole Pterodactyl node). */
    public int threadCount() {
        return threadCount;
    }

    public String pterodactylNode() {
        return pterodactylNode;
    }

    public String pterodactylServerId() {
        return pterodactylServerId;
    }

    public String pterodactylServerUuid() {
        return pterodactylServerUuid;
    }
}
