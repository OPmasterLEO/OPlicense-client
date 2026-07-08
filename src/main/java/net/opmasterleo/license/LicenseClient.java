package net.opmasterleo.license;

import net.opmasterleo.license.internal.HwidResolver;
import net.opmasterleo.license.internal.SimpleJson;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;
import java.security.SecureRandom;

public final class LicenseClient {
    private static final SecureRandom RANDOM = new SecureRandom();

    private final String apiUrl;
    private final String licenseKey;
    private final String product;
    private final ResponseVerifier verifier;
    private final HttpClient httpClient;

    private String hwid = defaultHwid();
    private String macAddress;
    private String productVersion;
    private String operatingSystem = System.getProperty("os.name");
    private String operatingSystemVersion = System.getProperty("os.version");
    private String operatingSystemArchitecture = System.getProperty("os.arch");
    private String javaVersion = System.getProperty("java.version");
    private String serverSoftware;
    private String serverSoftwareVersion;
    private String container;

    /** Legacy HMAC mode — prefer {@link #withEd25519(String, String, String, String)}. */
    public LicenseClient(String apiUrl, String licenseKey, String product, String hmacSecret) {
        this(apiUrl, licenseKey, product, ResponseVerifier.hmac(hmacSecret));
    }

    public LicenseClient(String apiUrl, String licenseKey, String product, ResponseVerifier verifier) {
        if (apiUrl == null || licenseKey == null || product == null || verifier == null) {
            throw new LicenseException("apiUrl, licenseKey, product, and verifier are all required");
        }
        this.apiUrl = apiUrl;
        this.licenseKey = licenseKey;
        this.product = product;
        this.verifier = verifier;
        this.httpClient = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();
    }

    public static LicenseClient withEd25519(
            String apiUrl,
            String licenseKey,
            String product,
            String ed25519PublicKeySpkiBase64
    ) {
        return new LicenseClient(apiUrl, licenseKey, product, ResponseVerifier.ed25519(ed25519PublicKeySpkiBase64));
    }

    public LicenseClient setHwid(String hwid) {
        this.hwid = hwid;
        return this;
    }

    /** Recomputes and applies the SDK's stable HWID strategy. */
    public LicenseClient useAutoHwid() {
        this.hwid = defaultHwid();
        return this;
    }

    public LicenseClient setMacAddress(String macAddress) {
        this.macAddress = macAddress;
        return this;
    }

    public LicenseClient setProductVersion(String productVersion) {
        this.productVersion = productVersion;
        return this;
    }

    public LicenseClient setServerSoftware(String serverSoftware, String serverSoftwareVersion) {
        this.serverSoftware = serverSoftware;
        this.serverSoftwareVersion = serverSoftwareVersion;
        return this;
    }

    public LicenseClient setContainer(String container) {
        this.container = container;
        return this;
    }

    public ValidationRequest validate() {
        return new ValidationRequest(this);
    }

    LicenseResult execute() {
        String requestNonce = randomNonce();
        Map<String, Object> fields = new LinkedHashMap<>();
        fields.put("timestamp", System.currentTimeMillis() / 1000);
        fields.put("nonce", requestNonce);
        fields.put("hwid", hwid);
        fields.put("macAddress", macAddress);
        fields.put("productVersion", productVersion);
        fields.put("operatingSystem", operatingSystem);
        fields.put("operatingSystemVersion", operatingSystemVersion);
        fields.put("operatingSystemArchitecture", operatingSystemArchitecture);
        fields.put("javaVersion", javaVersion);
        fields.put("serverSoftware", serverSoftware);
        fields.put("serverSoftwareVersion", serverSoftwareVersion);
        fields.put("container", container);

        String requestBody = SimpleJson.object(fields);
        String url = apiUrl + "/v1/license/" + encodePathSegment(product) + "/" + encodePathSegment(licenseKey);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Content-Type", "application/json")
                .timeout(Duration.ofSeconds(15))
                .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                .build();

        HttpResponse<String> response = null;
        int maxAttempts = 3;
        long backoffBaseMs = 500;

        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            try {
                response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
                break;
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return new LicenseResult(LicenseOutcome.NETWORK_ERROR, product, null, null, null, e);
            } catch (IOException e) {
                if (attempt >= maxAttempts) {
                    return new LicenseResult(LicenseOutcome.NETWORK_ERROR, product, null, null, null, e);
                }
                try {
                    long sleepMs = backoffBaseMs * attempt;
                    Thread.sleep(sleepMs);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    return new LicenseResult(LicenseOutcome.NETWORK_ERROR, product, null, null, null, ie);
                }
            }
        }

        if (response == null) {
            return new LicenseResult(
                    LicenseOutcome.NETWORK_ERROR,
                    product,
                    null,
                    null,
                    null,
                    new IOException("No response received")
            );
        }

        final HttpResponse<String> finalResponse = response;
        String responseBody = finalResponse.body();
        int statusCode = finalResponse.statusCode();

        if (statusCode >= 500) {
            return new LicenseResult(LicenseOutcome.NETWORK_ERROR, product, null, null, responseBody, null);
        }

        String signature = finalResponse.headers().firstValue("x-signature")
                .or(() -> finalResponse.headers().firstValue("X-Signature"))
                .orElse(null);
        if (signature != null) {
            signature = signature.trim();
        }

        String algorithm = finalResponse.headers().firstValue("x-signature-alg")
                .or(() -> finalResponse.headers().firstValue("X-Signature-Alg"))
                .orElse(null);

        if (!verifier.verify(responseBody, signature, algorithm)) {
            return new LicenseResult(LicenseOutcome.SIGNATURE_INVALID, product, null, null, responseBody, null);
        }

        Map<String, String> parsed = SimpleJson.parseFlat(responseBody);
        String echoedNonce = parsed.get("requestNonce");
        if (!requestNonce.equals(echoedNonce)) {
            return new LicenseResult(LicenseOutcome.RESPONSE_INVALID, product, null, null, responseBody, null);
        }

        Long issuedAt = parseLongOrNull(parsed.get("issuedAt"));
        if (issuedAt == null) {
            return new LicenseResult(LicenseOutcome.RESPONSE_INVALID, product, null, null, responseBody, null);
        }
        long now = System.currentTimeMillis() / 1000;
        if (Math.abs(now - issuedAt) > 120) {
            return new LicenseResult(LicenseOutcome.RESPONSE_INVALID, product, null, null, responseBody, null);
        }

        String responseProduct = parsed.get("product");
        if (responseProduct != null && !responseProduct.equals(product)) {
            return new LicenseResult(LicenseOutcome.RESPONSE_INVALID, product, null, null, responseBody, null);
        }

        boolean valid = "true".equals(parsed.get("valid"));
        String status = parsed.get("status");
        String expiresAt = parsed.get("expiresAt");
        String ownerDiscordId = parsed.get("ownerDiscordId");
        String serverId = parsed.get("serverId");
        String[] whitelistedIps = SimpleJson.parseStringArray(responseBody, "whitelistedIps");

        if (valid) {
            return new LicenseResult(
                    LicenseOutcome.VALID,
                    product,
                    status,
                    expiresAt,
                    ownerDiscordId,
                    serverId,
                    whitelistedIps,
                    responseBody,
                    null
            );
        }

        LicenseOutcome outcome = mapReason(parsed.get("reason"));
        return new LicenseResult(
                outcome,
                product,
                status,
                expiresAt,
                ownerDiscordId,
                serverId,
                whitelistedIps,
                responseBody,
                null
        );
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

    private static String randomNonce() {
        byte[] raw = new byte[24];
        RANDOM.nextBytes(raw);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(raw);
    }

    private static String defaultHwid() {
        return HwidResolver.resolveStable();
    }

    private static String encodePathSegment(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8).replace("+", "%20");
    }
}
