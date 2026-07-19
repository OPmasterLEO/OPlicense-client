package net.opmasterleo.license.internal.security;

import net.opmasterleo.license.internal.core.ClientConfig;
import net.opmasterleo.license.internal.core.RequestContext;
import net.opmasterleo.license.internal.json.Json;
import net.opmasterleo.license.internal.util.Numbers;
import net.opmasterleo.license.internal.util.Strings;
import net.opmasterleo.license.model.LicenseEnvironment;
import net.opmasterleo.license.model.LicenseOutcome;
import net.opmasterleo.license.model.LicenseResult;
import net.opmasterleo.license.model.LicenseUpdate;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Map;

public final class ResponseGuard {

    private static final long MAX_CLOCK_SKEW_SECONDS = 120L;

    public ResponseGuard() {
    }

    public LicenseResult verifyAndParse(
            ClientConfig config,
            RequestContext context,
            LicenseEnvironment environment,
            String requestNonce,
            String responseBody,
            String signature,
            String algorithm
    ) {
        String product = config.product();

        if (responseBody == null || responseBody.isBlank()) {
            return failure(LicenseOutcome.RESPONSE_INVALID, product, environment, context, responseBody);
        }

        if (!config.verifier().verify(responseBody, signature, algorithm)) {
            return failure(LicenseOutcome.SIGNATURE_INVALID, product, environment, context, responseBody);
        }

        Map<String, String> parsed = Json.parseFlat(responseBody);
        if (!constantTimeEquals(requestNonce, parsed.get("requestNonce"))) {
            return failure(LicenseOutcome.RESPONSE_INVALID, product, environment, context, responseBody);
        }

        Long issuedAt = Numbers.parseLongOrNull(parsed.get("issuedAt"));
        if (issuedAt == null) {
            return failure(LicenseOutcome.RESPONSE_INVALID, product, environment, context, responseBody);
        }

        long now = System.currentTimeMillis() / 1000L;
        if (Math.abs(now - issuedAt.longValue()) > MAX_CLOCK_SKEW_SECONDS) {
            return failure(LicenseOutcome.RESPONSE_INVALID, product, environment, context, responseBody);
        }

        String responseProduct = parsed.get("product");
        if (responseProduct != null && !responseProduct.equals(product)) {
            return failure(LicenseOutcome.RESPONSE_INVALID, product, environment, context, responseBody);
        }

        boolean valid = "true".equals(parsed.get("valid"));
        String status = parsed.get("status");
        String expiresAt = parsed.get("expiresAt");
        String owner = Strings.firstNonBlank(parsed.get("owner"), parsed.get("ownerUsername"));
        String ownerDiscordId = parsed.get("ownerDiscordId");
        String serverId = parsed.get("serverId");
        String[] whitelistedIps = Json.parseStringArray(responseBody, "whitelistedIps");
        LicenseUpdate update = LicenseUpdate.of(
                context.productVersion(),
                parsed.get("latestPluginVersion"),
                "true".equals(parsed.get("updateAvailable"))
        );

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

    private static LicenseResult failure(
            LicenseOutcome outcome,
            String product,
            LicenseEnvironment environment,
            RequestContext context,
            String rawBody
    ) {
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
                LicenseUpdate.none(context.productVersion()),
                rawBody,
                null
        );
    }

    private static boolean constantTimeEquals(String left, String right) {
        if (left == null || right == null) {
            return false;
        }
        byte[] a = left.getBytes(StandardCharsets.UTF_8);
        byte[] b = right.getBytes(StandardCharsets.UTF_8);
        return MessageDigest.isEqual(a, b);
    }

    static LicenseOutcome mapReason(String reason) {
        if (reason == null) {
            return LicenseOutcome.NETWORK_ERROR;
        }
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
}
