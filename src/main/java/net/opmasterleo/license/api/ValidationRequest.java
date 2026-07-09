package net.opmasterleo.license.api;

import net.opmasterleo.license.LicenseClient;
import net.opmasterleo.license.model.LicenseOutcome;
import net.opmasterleo.license.model.LicenseResult;

import java.util.function.Consumer;

public final class ValidationRequest {

    private final LicenseClient client;

    private Runnable onValid;
    private Consumer<LicenseResult> onValidWithResult;
    private Consumer<LicenseResult> onExpired;
    private Consumer<LicenseResult> onRevoked;
    private Consumer<LicenseResult> onDeactivated;
    private Consumer<LicenseResult> onDeleted;
    private Consumer<LicenseResult> onIpNotWhitelisted;
    private Consumer<LicenseResult> onHwidRequired;
    private Consumer<LicenseResult> onMaxHwidExceeded;
    private Consumer<LicenseResult> onBlacklistedIp;
    private Consumer<LicenseResult> onBlacklistedHwid;
    private Consumer<LicenseResult> onProductMismatch;
    private Consumer<LicenseResult> onProductArchived;
    private Consumer<LicenseResult> onLicenseNotFound;
    private Consumer<LicenseResult> onTimestampDesync;
    private Consumer<LicenseResult> onRateLimited;
    private Consumer<LicenseResult> onSignatureInvalid;
    private Consumer<Exception> onNetworkError;

    public ValidationRequest(LicenseClient client) {
        this.client = client;
    }

    public ValidationRequest onValid(Runnable callback) {
        this.onValid = callback;
        return this;
    }

    public ValidationRequest onValid(Consumer<LicenseResult> callback) {
        this.onValidWithResult = callback;
        return this;
    }

    public ValidationRequest onExpired(Consumer<LicenseResult> callback) {
        this.onExpired = callback;
        return this;
    }

    public ValidationRequest onRevoked(Consumer<LicenseResult> callback) {
        this.onRevoked = callback;
        return this;
    }

    public ValidationRequest onDeactivated(Consumer<LicenseResult> callback) {
        this.onDeactivated = callback;
        return this;
    }

    public ValidationRequest onDeleted(Consumer<LicenseResult> callback) {
        this.onDeleted = callback;
        return this;
    }

    public ValidationRequest onIpNotWhitelisted(Consumer<LicenseResult> callback) {
        this.onIpNotWhitelisted = callback;
        return this;
    }

    public ValidationRequest onHwidRequired(Consumer<LicenseResult> callback) {
        this.onHwidRequired = callback;
        return this;
    }

    public ValidationRequest onMaxHwidExceeded(Consumer<LicenseResult> callback) {
        this.onMaxHwidExceeded = callback;
        return this;
    }

    public ValidationRequest onBlacklistedIp(Consumer<LicenseResult> callback) {
        this.onBlacklistedIp = callback;
        return this;
    }

    public ValidationRequest onBlacklistedHwid(Consumer<LicenseResult> callback) {
        this.onBlacklistedHwid = callback;
        return this;
    }

    public ValidationRequest onProductMismatch(Consumer<LicenseResult> callback) {
        this.onProductMismatch = callback;
        return this;
    }

    public ValidationRequest onProductArchived(Consumer<LicenseResult> callback) {
        this.onProductArchived = callback;
        return this;
    }

    public ValidationRequest onLicenseNotFound(Consumer<LicenseResult> callback) {
        this.onLicenseNotFound = callback;
        return this;
    }

    public ValidationRequest onTimestampDesync(Consumer<LicenseResult> callback) {
        this.onTimestampDesync = callback;
        return this;
    }

    public ValidationRequest onRateLimited(Consumer<LicenseResult> callback) {
        this.onRateLimited = callback;
        return this;
    }

    public ValidationRequest onSignatureInvalid(Consumer<LicenseResult> callback) {
        this.onSignatureInvalid = callback;
        return this;
    }

    public ValidationRequest onNetworkError(Consumer<Exception> callback) {
        this.onNetworkError = callback;
        return this;
    }

    public void run() {
        LicenseResult result = client.execute();
        dispatch(result);
    }

    private void dispatch(LicenseResult result) {
        switch (result.outcome()) {
            case VALID:
                if (onValidWithResult != null) onValidWithResult.accept(result);
                else if (onValid != null) onValid.run();
                break;
            case EXPIRED:
                if (onExpired != null) onExpired.accept(result);
                break;
            case REVOKED:
                if (onRevoked != null) onRevoked.accept(result);
                break;
            case DEACTIVATED:
                if (onDeactivated != null) onDeactivated.accept(result);
                break;
            case DELETED:
                if (onDeleted != null) onDeleted.accept(result);
                break;
            case IP_NOT_WHITELISTED:
                if (onIpNotWhitelisted != null) onIpNotWhitelisted.accept(result);
                break;
            case HWID_REQUIRED:
                if (onHwidRequired != null) onHwidRequired.accept(result);
                break;
            case MAX_HWIDS_REACHED:
                if (onMaxHwidExceeded != null) onMaxHwidExceeded.accept(result);
                break;
            case BLACKLISTED_IP:
                if (onBlacklistedIp != null) onBlacklistedIp.accept(result);
                break;
            case BLACKLISTED_HWID:
                if (onBlacklistedHwid != null) onBlacklistedHwid.accept(result);
                break;
            case PRODUCT_MISMATCH:
                if (onProductMismatch != null) onProductMismatch.accept(result);
                break;
            case PRODUCT_ARCHIVED:
                if (onProductArchived != null) onProductArchived.accept(result);
                break;
            case LICENSE_NOT_FOUND:
                if (onLicenseNotFound != null) onLicenseNotFound.accept(result);
                break;
            case TIMESTAMP_DESYNC:
                if (onTimestampDesync != null) onTimestampDesync.accept(result);
                break;
            case RATE_LIMITED:
                if (onRateLimited != null) onRateLimited.accept(result);
                break;
            case NONCE_INVALID:
            case RESPONSE_INVALID:
            case SIGNATURE_INVALID:
                if (onSignatureInvalid != null) onSignatureInvalid.accept(result);
                break;
            case NETWORK_ERROR:
                if (onNetworkError != null) onNetworkError.accept(result.networkError());
                break;
        }
    }
}
