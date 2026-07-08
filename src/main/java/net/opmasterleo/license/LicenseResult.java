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
}
