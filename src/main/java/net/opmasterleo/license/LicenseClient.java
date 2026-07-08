package net.opmasterleo.license;

import net.opmasterleo.license.internal.SimpleJson;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;

public final class LicenseClient {

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
        Map<String, Object> fields = new LinkedHashMap<>();
        fields.put("timestamp", System.currentTimeMillis() / 1000);
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

        String responseBody = response.body();
        int statusCode = response.statusCode();

        if (statusCode >= 500) {
            return new LicenseResult(LicenseOutcome.NETWORK_ERROR, product, null, null, responseBody, null);
        }

        String signature = response.headers().firstValue("x-signature")
                .or(() -> response.headers().firstValue("X-Signature"))
                .orElse(null);
        if (signature != null) {
            signature = signature.trim();
        }

        String algorithm = response.headers().firstValue("x-signature-alg")
                .or(() -> response.headers().firstValue("X-Signature-Alg"))
                .orElse(null);

        if (!verifier.verify(responseBody, signature, algorithm)) {
            return new LicenseResult(LicenseOutcome.SIGNATURE_INVALID, product, null, null, responseBody, null);
        }

        Map<String, String> parsed = SimpleJson.parseFlat(responseBody);
        boolean valid = "true".equals(parsed.get("valid"));
        String status = parsed.get("status");
        String expiresAt = parsed.get("expiresAt");

        if (valid) {
            return new LicenseResult(LicenseOutcome.VALID, product, status, expiresAt, responseBody, null);
        }

        LicenseOutcome outcome = mapReason(parsed.get("reason"));
        return new LicenseResult(outcome, product, status, expiresAt, responseBody, null);
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
            default:
                return LicenseOutcome.NETWORK_ERROR;
        }
    }

    private static String defaultHwid() {
        try {
            java.net.NetworkInterface ni = java.net.NetworkInterface.getByInetAddress(java.net.InetAddress.getLocalHost());
            if (ni != null && ni.getHardwareAddress() != null) {
                StringBuilder sb = new StringBuilder();
                for (byte b : ni.getHardwareAddress()) {
                    sb.append(String.format("%02X", b));
                }
                return sb.toString();
            }
        } catch (Exception ignored) {
        }
        return System.getProperty("user.name", "unknown") + "-" + System.getProperty("os.name", "unknown");
    }

    private static String encodePathSegment(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8).replace("+", "%20");
    }
}
