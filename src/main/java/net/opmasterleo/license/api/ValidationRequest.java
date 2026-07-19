package net.opmasterleo.license.api;

import net.opmasterleo.license.LicenseClient;
import net.opmasterleo.license.model.LicenseResult;

/** Runs validation and dispatches the matching callback. */
public final class ValidationRequest {

    private final LicenseClient client;

    /** Creates a validation request for the given client. */
    public ValidationRequest(LicenseClient client) {
        this.client = client;
    }

    /** Validates and invokes the matching callback. */
    public void run(ValidationCallbacks callbacks) {
        if (callbacks == null) {
            throw new IllegalArgumentException("callbacks is required");
        }
        LicenseResult result = client.execute();
        dispatch(result, callbacks);
    }

    void dispatch(LicenseResult result, ValidationCallbacks callbacks) {
        switch (result.outcome()) {
            case VALID:
                callbacks.onValid(result);
                break;
            case EXPIRED:
                callbacks.onExpired(result);
                break;
            case REVOKED:
                callbacks.onRevoked(result);
                break;
            case DEACTIVATED:
                callbacks.onDeactivated(result);
                break;
            case DELETED:
                callbacks.onDeleted(result);
                break;
            case IP_NOT_WHITELISTED:
                callbacks.onIpNotWhitelisted(result);
                break;
            case HWID_REQUIRED:
                callbacks.onHwidRequired(result);
                break;
            case MAX_HWIDS_REACHED:
                callbacks.onMaxHwidExceeded(result);
                break;
            case BLACKLISTED_IP:
                callbacks.onBlacklistedIp(result);
                break;
            case BLACKLISTED_HWID:
                callbacks.onBlacklistedHwid(result);
                break;
            case PRODUCT_MISMATCH:
                callbacks.onProductMismatch(result);
                break;
            case PRODUCT_ARCHIVED:
                callbacks.onProductArchived(result);
                break;
            case LICENSE_NOT_FOUND:
                callbacks.onLicenseNotFound(result);
                break;
            case TIMESTAMP_DESYNC:
                callbacks.onTimestampDesync(result);
                break;
            case RATE_LIMITED:
                callbacks.onRateLimited(result);
                break;
            case NONCE_INVALID:
            case RESPONSE_INVALID:
            case SIGNATURE_INVALID:
                callbacks.onSignatureInvalid(result);
                break;
            case NETWORK_ERROR:
                callbacks.onNetworkError(result.networkErrorCause());
                break;
            default:
                break;
        }
    }
}
