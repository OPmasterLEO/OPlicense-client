package net.opmasterleo.license.internal.transport;

import net.opmasterleo.license.api.ResponseVerifier;

import java.net.http.HttpClient;
import java.time.Duration;

public final class LicenseConnection {

    private final String apiUrl;
    private final String licenseKey;
    private final String product;
    private final ResponseVerifier verifier;
    private final HttpClient httpClient;

    public LicenseConnection(String apiUrl, String licenseKey, String product, ResponseVerifier verifier) {
        if (apiUrl == null || licenseKey == null || product == null || verifier == null) {
            throw new IllegalArgumentException("apiUrl, licenseKey, product, and verifier are all required");
        }
        this.apiUrl = normalizeApiUrl(apiUrl);
        this.licenseKey = licenseKey;
        this.product = product;
        this.verifier = verifier;
        this.httpClient = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();
    }

    private static String normalizeApiUrl(String apiUrl) {
        String trimmed = apiUrl.trim();
        while (trimmed.endsWith("/")) {
            trimmed = trimmed.substring(0, trimmed.length() - 1);
        }
        if (trimmed.isEmpty()) {
            throw new IllegalArgumentException("apiUrl must not be empty");
        }
        return trimmed;
    }

    String apiUrl() {
        return apiUrl;
    }

    String licenseKey() {
        return licenseKey;
    }

    String product() {
        return product;
    }

    ResponseVerifier verifier() {
        return verifier;
    }

    HttpClient httpClient() {
        return httpClient;
    }
}
