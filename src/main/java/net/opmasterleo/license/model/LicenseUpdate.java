package net.opmasterleo.license.model;

public final class LicenseUpdate {

    private final String currentVersion;
    private final String latestVersion;
    private final boolean updateAvailable;

    public LicenseUpdate(String currentVersion, String latestVersion, boolean updateAvailable) {
        this.currentVersion = currentVersion;
        this.latestVersion = latestVersion;
        this.updateAvailable = updateAvailable;
    }

    public static LicenseUpdate of(String currentVersion, String latestVersion, boolean updateAvailable) {
        return new LicenseUpdate(currentVersion, latestVersion, updateAvailable);
    }

    public static LicenseUpdate none(String currentVersion) {
        return new LicenseUpdate(currentVersion, null, false);
    }

    public String currentVersion() {
        return currentVersion;
    }

    public String latestVersion() {
        return latestVersion;
    }

    public boolean updateAvailable() {
        return updateAvailable;
    }

    public String message() {
        if (!updateAvailable || latestVersion == null) {
            return null;
        }
        if (currentVersion == null || currentVersion.isBlank()) {
            return "A newer plugin version is available: v" + latestVersion + ".";
        }
        return "Update available: v" + latestVersion + " (running v" + currentVersion + ").";
    }
}
