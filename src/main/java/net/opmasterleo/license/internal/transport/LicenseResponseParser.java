package net.opmasterleo.license.internal.transport;

import net.opmasterleo.license.api.Ed25519ResponseVerifier;
import net.opmasterleo.license.internal.json.SimpleJson;
import net.opmasterleo.license.exception.LicenseException;
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
        Ed25519ResponseVerifier verifier = connection.verifier();
        String product = connection.product();

        if (!verifier.verify(responseBody, signature, algorithm)) {
            return reportableFailure(
                    connection, runtime, LicenseOutcome.SIGNATURE_INVALID, product, environment, responseBody, null);
        }

        Map<String, String> parsed = SimpleJson.parseFlat(responseBody);
        if (!requestNonce.equals(parsed.get("requestNonce"))) {
            return reportableFailure(
                    connection, runtime, LicenseOutcome.RESPONSE_INVALID, product, environment, responseBody, null);
        }

        Long issuedAt = parseLongOrNull(parsed.get("issuedAt"));
        if (issuedAt == null) {
            return reportableFailure(
                    connection, runtime, LicenseOutcome.RESPONSE_INVALID, product, environment, responseBody, null);
        }

        long now = System.currentTimeMillis() / 1000;
        if (Math.abs(now - issuedAt) > 120) {
            return reportableFailure(
                    connection, runtime, LicenseOutcome.RESPONSE_INVALID, product, environment, responseBody, null);
        }

        String responseProduct = parsed.get("product");
        if (responseProduct != null && !responseProduct.equals(product)) {
            return reportableFailure(
                    connection, runtime, LicenseOutcome.RESPONSE_INVALID, product, environment, responseBody, null);
        }

        boolean valid = "true".equals(parsed.get("valid"));
        String status = parsed.get("status");
        String expiresAt = parsed.get("expiresAt");
        String owner = firstNonBlank(parsed.get("owner"), parsed.get("ownerUsername"));
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
                    owner,
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
                owner,
                ownerDiscordId,
                serverId,
                whitelistedIps,
                environment,
                update,
                responseBody,
                null
        );
    }

    private LicenseResult reportableFailure(
            LicenseConnection connection,
            LicenseRuntime runtime,
            LicenseOutcome outcome,
            String product,
            LicenseEnvironment environment,
            String rawBody,
            Exception networkError
    ) {
        new LicenseHttpTransport().reportClientOutcome(connection, runtime, outcome);
        return failure(outcome, product, environment, runtime, rawBody, networkError);
    }

    LicenseResult failure(
            LicenseOutcome outcome,
            String product,
            LicenseEnvironment environment,
            LicenseRuntime runtime,
            String rawBody,
            Exception networkError
    ) {
        Exception resolvedError = networkError;
        if (outcome == LicenseOutcome.NETWORK_ERROR && resolvedError == null) {
            resolvedError = new LicenseException(
                    "Could not reach the OPLicense API. The license server may be offline or unreachable."
            );
        }

        return LicenseResult.create(
                outcome,
                product,
                null,
                null,
                null,
                null,
                null,
                null,
                environment,
                LicenseUpdate.of(runtime.productVersion(), null, false),
                rawBody,
                resolvedError
        );
    }

    private static String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) return value;
        }
        return null;
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
        if (value == null || value.isEmpty()) {
            return null;
        }

        int index = 0;
        boolean negative = false;
        if (value.charAt(0) == '-') {
            negative = true;
            index = 1;
        }
        if (index >= value.length()) {
            return null;
        }

        long result = 0;
        for (int i = index; i < value.length(); i++) {
            char digit = value.charAt(i);
            if (digit < '0' || digit > '9') {
                return null;
            }
            result = (result * 10L) + (digit - '0');
        }

        return negative ? -result : result;
    }
}
