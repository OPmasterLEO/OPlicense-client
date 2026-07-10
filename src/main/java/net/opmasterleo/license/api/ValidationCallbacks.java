package net.opmasterleo.license.api;

import net.opmasterleo.license.model.LicenseResult;

public abstract class ValidationCallbacks {

    public void onValid(LicenseResult result) {
    }

    public void onExpired(LicenseResult result) {
    }

    public void onRevoked(LicenseResult result) {
    }

    public void onDeactivated(LicenseResult result) {
    }

    public void onDeleted(LicenseResult result) {
    }

    public void onIpNotWhitelisted(LicenseResult result) {
    }

    public void onHwidRequired(LicenseResult result) {
    }

    public void onMaxHwidExceeded(LicenseResult result) {
    }

    public void onBlacklistedIp(LicenseResult result) {
    }

    public void onBlacklistedHwid(LicenseResult result) {
    }

    public void onProductMismatch(LicenseResult result) {
    }

    public void onProductArchived(LicenseResult result) {
    }

    public void onLicenseNotFound(LicenseResult result) {
    }

    public void onTimestampDesync(LicenseResult result) {
    }

    public void onRateLimited(LicenseResult result) {
    }

    public void onSignatureInvalid(LicenseResult result) {
    }

    public void onNetworkError(Exception error) {
    }
}
