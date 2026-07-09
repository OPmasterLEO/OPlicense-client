package net.opmasterleo.license.model;

public final class LicenseResult {

    private final LicenseOutcome outcome;
    private final String product;
    private final String status;
    private final String expiresAt;
    private final String owner;
    private final String ownerDiscordId;
    private final String serverId;
    private final String[] whitelistedIps;
    private final LicenseEnvironment environment;
    private final LicenseUpdate update;
    private final String rawBody;
    private final Exception networkError;

    public LicenseResult(LicenseOutcome outcome, String product, String status, String expiresAt, String rawBody, Exception networkError) {
        this(outcome, product, status, expiresAt, null, null, null, null, null, null, rawBody, networkError);
    }

    public LicenseResult(
            LicenseOutcome outcome,
            String product,
            String status,
            String expiresAt,
            String ownerDiscordId,
            String serverId,
            String[] whitelistedIps,
            String rawBody,
            Exception networkError
    ) {
        this(outcome, product, status, expiresAt, null, ownerDiscordId, serverId, whitelistedIps, null, null, rawBody, networkError);
    }

    public LicenseResult(
            LicenseOutcome outcome,
            String product,
            String status,
            String expiresAt,
            String ownerDiscordId,
            String serverId,
            String[] whitelistedIps,
            LicenseEnvironment environment,
            String rawBody,
            Exception networkError
    ) {
        this(outcome, product, status, expiresAt, null, ownerDiscordId, serverId, whitelistedIps, environment, null, rawBody, networkError);
    }

    public LicenseResult(
            LicenseOutcome outcome,
            String product,
            String status,
            String expiresAt,
            String owner,
            String ownerDiscordId,
            String serverId,
            String[] whitelistedIps,
            LicenseEnvironment environment,
            LicenseUpdate update,
            String rawBody,
            Exception networkError
    ) {
        this.outcome = outcome;
        this.product = product;
        this.status = status;
        this.expiresAt = expiresAt;
        this.owner = owner;
        this.ownerDiscordId = ownerDiscordId;
        this.serverId = serverId;
        this.whitelistedIps = whitelistedIps == null ? new String[0] : whitelistedIps.clone();
        this.environment = environment;
        this.update = update == null ? LicenseUpdate.of(null, null, false) : update;
        this.rawBody = rawBody;
        this.networkError = networkError;
    }

    public static LicenseResult create(
            LicenseOutcome outcome,
            String product,
            String status,
            String expiresAt,
            String owner,
            String ownerDiscordId,
            String serverId,
            String[] whitelistedIps,
            LicenseEnvironment environment,
            LicenseUpdate update,
            String rawBody,
            Exception networkError
    ) {
        return new LicenseResult(
                outcome,
                product,
                status,
                expiresAt,
                owner,
                ownerDiscordId,
                serverId,
                whitelistedIps,
                environment,
                update,
                rawBody,
                networkError
        );
    }

    public LicenseOutcome outcome() {
        return outcome;
    }

    public String product() {
        return product;
    }

    public String status() {
        return status;
    }

    public String expiresAt() {
        return expiresAt;
    }

    /** Discord display name when the API resolved it; otherwise falls back to the snowflake ID. */
    public String owner() {
        if (owner != null && !owner.isBlank()) return owner;
        return ownerDiscordId;
    }

    public String ownerDiscordId() {
        return ownerDiscordId;
    }

    public String serverId() {
        return serverId;
    }

    public String[] whitelistedIps() {
        return whitelistedIps.clone();
    }

    public LicenseEnvironment environment() {
        return environment;
    }

    public LicenseUpdate update() {
        return update;
    }

    public String rawBody() {
        return rawBody;
    }

    public Exception networkError() {
        return networkError;
    }

    public String summary() {
        String statusPart = status == null ? "" : " Status: " + status + ".";
        String expiresPart = formatExpiresAt();
        String updatePart = update.updateAvailable() ? " " + update.message() : "";

        switch (outcome) {
            case VALID:
                return "License valid." + (statusPart.isEmpty() ? "" : statusPart) + expiresPart + updatePart;
            case EXPIRED:
                return "License expired." + (statusPart.isEmpty() ? "" : statusPart) + expiresPart;
            case REVOKED:
                return "License revoked." + (statusPart.isEmpty() ? "" : statusPart) + expiresPart;
            case DEACTIVATED:
                return "License deactivated." + (statusPart.isEmpty() ? "" : statusPart) + expiresPart;
            case DELETED:
                return "License deleted." + (statusPart.isEmpty() ? "" : statusPart) + expiresPart;
            case IP_NOT_WHITELISTED:
                return "Server IP is not whitelisted for this license." + (statusPart.isEmpty() ? "" : statusPart) + expiresPart;
            case HWID_REQUIRED:
                return "A hardware identifier (HWID) is required to validate this license." + (statusPart.isEmpty() ? "" : statusPart) + expiresPart;
            case MAX_HWIDS_REACHED:
                return "This license has reached its maximum number of allowed HWIDs." + (statusPart.isEmpty() ? "" : statusPart) + expiresPart;
            case BLACKLISTED_IP:
                return "This server's IP is blacklisted for validation." + (statusPart.isEmpty() ? "" : statusPart) + expiresPart;
            case BLACKLISTED_HWID:
                return "This device's HWID is blacklisted for validation." + (statusPart.isEmpty() ? "" : statusPart) + expiresPart;
            case PRODUCT_MISMATCH:
                return "License is not valid for this product.";
            case PRODUCT_ARCHIVED:
                return "This product is no longer supported.";
            case LICENSE_NOT_FOUND:
                return "License key not recognized.";
            case TIMESTAMP_DESYNC:
                return "Request timestamp is out of sync with the server.";
            case RATE_LIMITED:
                return "Too many validation attempts; please try again shortly.";
            case NONCE_INVALID:
                return "Request nonce was rejected by the license server.";
            case RESPONSE_INVALID:
                return "License response failed local anti-replay checks.";
            case SIGNATURE_INVALID:
                return "License response could not be verified (signature mismatch). "
                        + "Confirm the Ed25519 public key from /product info is correct.";
            case NETWORK_ERROR:
                return "Could not reach the license server.";
            default:
                return "License outcome: " + outcome;
        }
    }

    private String formatExpiresAt() {
        if (expiresAt == null || "null".equalsIgnoreCase(expiresAt)) {
            if (outcome == LicenseOutcome.VALID || "ACTIVE".equals(status)) return " Expires: Lifetime.";
            return "";
        }
        return " Expires: " + expiresAt + ".";
    }
}
