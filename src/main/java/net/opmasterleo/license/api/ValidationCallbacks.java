package net.opmasterleo.license.api;

import net.opmasterleo.license.model.LicenseResult;

/** Callbacks for license validation outcomes. */
public abstract class ValidationCallbacks {

    /** Called when the license is valid. */
    public void onValid(LicenseResult result) {
    }

    /** Called when the license is expired. */
    public void onExpired(LicenseResult result) {
    }

    /** Called when the license is revoked. */
    public void onRevoked(LicenseResult result) {
    }

    /** Called when the license is deactivated. */
    public void onDeactivated(LicenseResult result) {
    }

    /** Called when the license is deleted. */
    public void onDeleted(LicenseResult result) {
    }

    /** Called when the server IP is not whitelisted. */
    public void onIpNotWhitelisted(LicenseResult result) {
    }

    /** Called when an HWID is required. */
    public void onHwidRequired(LicenseResult result) {
    }

    /** Called when the HWID limit is exceeded. */
    public void onMaxHwidExceeded(LicenseResult result) {
    }

    /** Called when the server IP is blacklisted. */
    public void onBlacklistedIp(LicenseResult result) {
    }

    /** Called when the HWID is blacklisted. */
    public void onBlacklistedHwid(LicenseResult result) {
    }

    /** Called when the license does not match the product. */
    public void onProductMismatch(LicenseResult result) {
    }

    /** Called when the product is archived. */
    public void onProductArchived(LicenseResult result) {
    }

    /** Called when the license key is not found. */
    public void onLicenseNotFound(LicenseResult result) {
    }

    /** Called when the request timestamp is out of sync. */
    public void onTimestampDesync(LicenseResult result) {
    }

    /** Called when validation is rate limited. */
    public void onRateLimited(LicenseResult result) {
    }

    /** Called when the response signature or anti-replay checks fail. */
    public void onSignatureInvalid(LicenseResult result) {
    }

    /** Called when the API cannot be reached. */
    public void onNetworkError(Exception error) {
    }
}
