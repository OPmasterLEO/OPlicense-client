package net.opmasterleo.license.internal.transport;

import net.opmasterleo.license.api.ResponseVerifier;
import net.opmasterleo.license.internal.json.SimpleJson;
import net.opmasterleo.license.internal.runtime.LicenseRuntime;
import net.opmasterleo.license.model.LicenseEnvironment;
import net.opmasterleo.license.model.LicenseOutcome;
import net.opmasterleo.license.model.LicenseResult;
import net.opmasterleo.license.model.LicenseUpdate;

import java.util.Map;

final class LicenseResponseParser {

    LicenseResult parseVerifiedResponse(
            LicenseConnection connection,
            LicenseRuntime runtime,
            LicenseEnvironment environment,
            String requestNonce,
            String responseBody,
            String signature,
            String algorithm
    ) {
        ResponseVerifier verifier = connection.verifier();
        String product = connection.product();

        if (!verifier.verify(responseBody, signature, algorithm)) {
            return failure(LicenseOutcome.SIGNATURE_INVALID, product, environment, runtime, responseBody, null);
        }

        Map<String, String> parsed = SimpleJson.parseFlat(responseBody);
        if (!requestNonce.equals(parsed.get("requestNonce"))) {
            return failure(LicenseOutcome.RESPONSE_INVALID, product, environment, runtime, responseBody, null);
        }

        Long issuedAt = parseLongOrNull(parsed.get("issuedAt"));
        if (issuedAt == null) {
            return failure(LicenseOutcome.RESPONSE_INVALID, product, environment, runtime, responseBody, null);
        }

        long now = System.currentTimeMillis() / 1000;
        if (Math.abs(now - issuedAt) > 120) {
            return failure(LicenseOutcome.RESPONSE_INVALID, product, environment, runtime, responseBody, null);
        }

        String responseProduct = parsed.get("product");
        if (responseProduct != null && !responseProduct.equals(product)) {
            return failure(LicenseOutcome.RESPONSE_INVALID, product, environment, runtime, responseBody, null);
        }

        boolean valid = "true".equals(parsed.get("valid"));
        String status = parsed.get("status");
        String expiresAt = parsed.get("expiresAt");
        String ownerDiscordId = parsed.get("ownerDiscordId");
        String serverId = parsed.get("serverId");
        String[] whitelistedIps = SimpleJson.parseStringArray(responseBody, "whitelistedIps");
        LicenseUpdate update = parseUpdate(runtime.productVersion(), parsed);

        if (valid) {
            return LicenseResult.create(
                    LicenseOutcome.VALID,
                    product,
                    status,
                    expiresAt,
                    ownerDiscordId,
                    serverId,
                    whitelistedIps,
                    environment,
                    update,
                    responseBody,
                    null
            );
        }

        return LicenseResult.create(
                mapReason(parsed.get("reason")),
                product,
                status,
                expiresAt,
                ownerDiscordId,
                serverId,
                whitelistedIps,
                environment,
                update,
                responseBody,
                null
        );
    }

    LicenseResult failure(
            LicenseOutcome outcome,
            String product,
            LicenseEnvironment environment,
            LicenseRuntime runtime,
            String rawBody,
            Exception networkError
    ) {
        return LicenseResult.create(
                outcome,
                product,
                null,
                null,
                null,
                null,
                null,
                environment,
                LicenseUpdate.of(runtime.productVersion(), null, false),
                rawBody,
                networkError
        );
    }

    private static LicenseUpdate parseUpdate(String currentVersion, Map<String, String> parsed) {
        String latestVersion = parsed.get("latestPluginVersion");
        boolean updateAvailable = "true".equals(parsed.get("updateAvailable"));
        return LicenseUpdate.of(currentVersion, latestVersion, updateAvailable);
    }

    private static LicenseOutcome mapReason(String reason) {
        if (reason == null) return LicenseOutcome.NETWORK_ERROR;
        switch (reason) {
            case "PRODUCT_NOT_FOUND":
                return LicenseOutcome.PRODUCT_MISMATCH;
            case "PRODUCT_ARCHIVED":
                return LicenseOutcome.PRODUCT_ARCHIVED;
            case "LICENSE_NOT_FOUND":
                return LicenseOutcome.LICENSE_NOT_FOUND;
            case "REVOKED":
                return LicenseOutcome.REVOKED;
            case "EXPIRED":
                return LicenseOutcome.EXPIRED;
            case "DEACTIVATED":
                return LicenseOutcome.DEACTIVATED;
            case "DELETED":
                return LicenseOutcome.DELETED;
            case "IP_NOT_WHITELISTED":
                return LicenseOutcome.IP_NOT_WHITELISTED;
            case "HWID_REQUIRED":
                return LicenseOutcome.HWID_REQUIRED;
            case "MAX_HWIDS_REACHED":
                return LicenseOutcome.MAX_HWIDS_REACHED;
            case "BLACKLISTED_IP":
                return LicenseOutcome.BLACKLISTED_IP;
            case "BLACKLISTED_HWID":
                return LicenseOutcome.BLACKLISTED_HWID;
            case "TIMESTAMP_DESYNC":
                return LicenseOutcome.TIMESTAMP_DESYNC;
            case "RATE_LIMITED":
                return LicenseOutcome.RATE_LIMITED;
            case "NONCE_INVALID":
                return LicenseOutcome.NONCE_INVALID;
            default:
                return LicenseOutcome.NETWORK_ERROR;
        }
    }

    private static Long parseLongOrNull(String value) {
        if (value == null) return null;
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException ignored) {
            return null;
        }
    }
}
