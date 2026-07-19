package net.opmasterleo.license.internal.core;

import net.opmasterleo.license.LicenseClient;
import net.opmasterleo.license.api.Ed25519ResponseVerifier;
import net.opmasterleo.license.internal.util.Strings;

public final class ClientConfig {

    private final String apiUrl;
    private final String licenseKey;
    private final String product;
    private final Ed25519ResponseVerifier verifier;

    private ClientConfig(String apiUrl, String licenseKey, String product, Ed25519ResponseVerifier verifier) {
        this.apiUrl = apiUrl;
        this.licenseKey = licenseKey;
        this.product = product;
        this.verifier = verifier;
    }

    public static ClientConfig create(
            String apiUrl,
            String licenseKey,
            String product,
            Ed25519ResponseVerifier verifier
    ) {
        if (verifier == null) {
            throw new IllegalArgumentException("verifier is required");
        }
        String normalizedUrl = normalizeApiUrl(apiUrl);
        String normalizedKey = requireNonBlank(licenseKey, "licenseKey");
        String normalizedProduct = requireNonBlank(product, "product");
        return new ClientConfig(normalizedUrl, normalizedKey, normalizedProduct, verifier);
    }

    private static String normalizeApiUrl(String apiUrl) {
        String trimmed = requireNonBlank(apiUrl, "apiUrl");
        while (trimmed.endsWith("/")) {
            trimmed = trimmed.substring(0, trimmed.length() - 1);
        }
        if (trimmed.isEmpty()) {
            throw new IllegalArgumentException("apiUrl must not be empty");
        }
        String lower = trimmed.toLowerCase();
        if (!lower.startsWith("https://") && !lower.startsWith("http://")) {
            throw new IllegalArgumentException("apiUrl must start with https:// or http://");
        }
        return trimmed;
    }

    private static String requireNonBlank(String value, String name) {
        if (value == null) {
            throw new IllegalArgumentException(name + " is required");
        }
        String trimmed = value.trim();
        if (trimmed.isEmpty()) {
            throw new IllegalArgumentException(name + " must not be blank");
        }
        return trimmed;
    }

    public String apiUrl() {
        return apiUrl;
    }

    public String licenseKey() {
        return licenseKey;
    }

    public String product() {
        return product;
    }

    public Ed25519ResponseVerifier verifier() {
        return verifier;
    }

    public String userAgent() {
        return "OPLicense-Client/" + LicenseClient.VERSION;
    }

    public String validateUrl() {
        return apiUrl
                + "/v1/license/"
                + Strings.encodePath(product)
                + "/"
                + Strings.encodePath(licenseKey);
    }

    public String clientOutcomeUrl() {
        return validateUrl() + "/client-outcome";
    }
}
