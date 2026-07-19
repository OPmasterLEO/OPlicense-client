package net.opmasterleo.license.model;

/** Plugin update information returned with a validation result. */
public final class LicenseUpdate {

    private final String currentVersion;
    private final String latestVersion;
    private final boolean updateAvailable;

    /** Creates an update snapshot. */
    public LicenseUpdate(String currentVersion, String latestVersion, boolean updateAvailable) {
        this.currentVersion = currentVersion;
        this.latestVersion = latestVersion;
        this.updateAvailable = updateAvailable;
    }

    /** Creates an update snapshot. */
    public static LicenseUpdate of(String currentVersion, String latestVersion, boolean updateAvailable) {
        return new LicenseUpdate(currentVersion, latestVersion, updateAvailable);
    }

    /** Creates an empty update snapshot for the current version. */
    public static LicenseUpdate none(String currentVersion) {
        return new LicenseUpdate(currentVersion, null, false);
    }

    /** Returns the version reported by the plugin. */
    public String currentVersion() {
        return currentVersion;
    }

    /** Returns the latest known version. */
    public String latestVersion() {
        return latestVersion;
    }

    /** Returns whether an update is available. */
    public boolean updateAvailable() {
        return updateAvailable;
    }

    /** Returns a short update message, or {@code null} when none. */
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
