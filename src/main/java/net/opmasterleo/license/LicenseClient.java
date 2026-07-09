package net.opmasterleo.license;

import net.opmasterleo.license.internal.EnvironmentResolver;
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
    private String userDir = EnvironmentResolver.resolveUserDir();
    private String userHome = EnvironmentResolver.resolveUserHome();
    private String userName = EnvironmentResolver.resolveUserName();
    private String pterodactylNode;

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

    public LicenseClient setUserDir(String userDir) {
        this.userDir = userDir;
        return this;
    }

    public LicenseClient setUserHome(String userHome) {
        this.userHome = userHome;
        return this;
    }

    public LicenseClient setUserName(String userName) {
        this.userName = userName;
        return this;
    }

    public LicenseClient setPterodactylNode(String pterodactylNode) {
        this.pterodactylNode = pterodactylNode;
        return this;
    }

    /** Captures the environment snapshot that would be sent on the next validate call. */
    public LicenseEnvironment environment() {
        return LicenseEnvironment.capture(userDir, userHome, userName, container, pterodactylNode);
    }

    public ValidationRequest validate() {
        return new ValidationRequest(this);
    }

    LicenseResult execute() {
        LicenseEnvironment environment = environment();
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
        fields.put("container", environment.container());
        fields.put("userDir", environment.userDir());
        fields.put("userHome", environment.userHome());
        fields.put("userName", environment.userName());
        fields.put("cpuCores", environment.cpuCores());
        fields.put("threadCount", environment.threadCount());
        if (environment.pterodactylNode() != null) fields.put("pterodactylNode", environment.pterodactylNode());
        if (environment.pterodactylServerId() != null) fields.put("pterodactylServerId", environment.pterodactylServerId());
        if (environment.pterodactylServerUuid() != null) fields.put("pterodactylServerUuid", environment.pterodactylServerUuid());

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
                return resultWithEnvironment(LicenseOutcome.NETWORK_ERROR, null, e, environment);
            } catch (IOException e) {
                if (attempt >= maxAttempts) {
                    return resultWithEnvironment(LicenseOutcome.NETWORK_ERROR, null, e, environment);
                }
                try {
                    long sleepMs = backoffBaseMs * attempt;
                    Thread.sleep(sleepMs);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    return resultWithEnvironment(LicenseOutcome.NETWORK_ERROR, null, ie, environment);
                }
            }
        }

        if (response == null) {
            return resultWithEnvironment(
                    LicenseOutcome.NETWORK_ERROR,
                    null,
                    new IOException("No response received"),
                    environment
            );
        }

        final HttpResponse<String> finalResponse = response;
        String responseBody = finalResponse.body();
        int statusCode = finalResponse.statusCode();

        if (statusCode >= 500) {
            return resultWithEnvironment(LicenseOutcome.NETWORK_ERROR, responseBody, null, environment);
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
            return resultWithEnvironment(LicenseOutcome.SIGNATURE_INVALID, responseBody, null, environment);
        }

        Map<String, String> parsed = SimpleJson.parseFlat(responseBody);
        String echoedNonce = parsed.get("requestNonce");
        if (!requestNonce.equals(echoedNonce)) {
            return resultWithEnvironment(LicenseOutcome.RESPONSE_INVALID, responseBody, null, environment);
        }

        Long issuedAt = parseLongOrNull(parsed.get("issuedAt"));
        if (issuedAt == null) {
            return resultWithEnvironment(LicenseOutcome.RESPONSE_INVALID, responseBody, null, environment);
        }
        long now = System.currentTimeMillis() / 1000;
        if (Math.abs(now - issuedAt) > 120) {
            return resultWithEnvironment(LicenseOutcome.RESPONSE_INVALID, responseBody, null, environment);
        }

        String responseProduct = parsed.get("product");
        if (responseProduct != null && !responseProduct.equals(product)) {
            return resultWithEnvironment(LicenseOutcome.RESPONSE_INVALID, responseBody, null, environment);
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
                    environment,
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
                environment,
                responseBody,
                null
        );
    }

    private LicenseResult resultWithEnvironment(
            LicenseOutcome outcome,
            String rawBody,
            Exception networkError,
            LicenseEnvironment environment
    ) {
        return new LicenseResult(
                outcome,
                product,
                null,
                null,
                null,
                null,
                null,
                environment,
                rawBody,
                networkError
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
