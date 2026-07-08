package net.opmasterleo.license;

public final class LicenseResult {

    private final LicenseOutcome outcome;
    private final String product;
    private final String status;
    private final String expiresAt;
    private final String rawBody;
    private final Exception networkError;

    LicenseResult(LicenseOutcome outcome, String product, String status, String expiresAt, String rawBody, Exception networkError) {
        this.outcome = outcome;
        this.product = product;
        this.status = status;
        this.expiresAt = expiresAt;
        this.rawBody = rawBody;
        this.networkError = networkError;
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

    public String rawBody() {
        return rawBody;
    }

    public Exception networkError() {
        return networkError;
    }

    public String summary() {
        String statusPart = status == null ? "" : " Status: " + status + ".";
        String expiresPart = formatExpiresAt();

        switch (outcome) {
            case VALID:
                return "License valid." + (statusPart.isEmpty() ? "" : statusPart) + expiresPart;
            case EXPIRED:
                return "License expired." + (statusPart.isEmpty() ? "" : statusPart) + expiresPart;
            case REVOKED:
                return "License revoked." + (statusPart.isEmpty() ? "" : statusPart) + expiresPart;
            case IP_NOT_WHITELISTED:
                return "Server IP is not whitelisted for this license." + (statusPart.isEmpty() ? "" : statusPart) + expiresPart;
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
            case SIGNATURE_INVALID:
                return "License response could not be verified (signature mismatch).";
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
