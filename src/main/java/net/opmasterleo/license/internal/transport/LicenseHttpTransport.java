package net.opmasterleo.license.internal.transport;

import net.opmasterleo.license.internal.json.SimpleJson;
import net.opmasterleo.license.internal.runtime.LicenseRuntime;
import net.opmasterleo.license.model.LicenseOutcome;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;

final class LicenseHttpTransport {

    HttpResponse<String> post(LicenseConnection connection, String requestBody) throws IOException, InterruptedException {
        String url = connection.apiUrl()
                + "/v1/license/"
                + encodePathSegment(connection.product())
                + "/"
                + encodePathSegment(connection.licenseKey());

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Content-Type", "application/json")
                .timeout(Duration.ofSeconds(15))
                .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                .build();

        IOException lastIo = null;
        int maxAttempts = 3;
        long backoffBaseMs = 500;

        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            try {
                return connection.httpClient().send(request, HttpResponse.BodyHandlers.ofString());
            } catch (InterruptedException e) {
                throw e;
            } catch (IOException e) {
                lastIo = e;
                if (attempt >= maxAttempts) break;
                try {
                    Thread.sleep(backoffBaseMs * attempt);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    throw ie;
                }
            }
        }

        throw lastIo != null ? lastIo : new IOException("No response received");
    }

    void reportClientOutcome(LicenseConnection connection, LicenseRuntime runtime, LicenseOutcome outcome) {
        String outcomeCode = toClientOutcome(outcome);
        if (outcomeCode == null) return;

        Map<String, Object> fields = new LinkedHashMap<>(runtime.toRequestFields());
        fields.put("outcome", outcomeCode);
        String requestBody = SimpleJson.object(fields);

        String url = connection.apiUrl()
                + "/v1/license/"
                + encodePathSegment(connection.product())
                + "/"
                + encodePathSegment(connection.licenseKey())
                + "/client-outcome";

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Content-Type", "application/json")
                .timeout(Duration.ofSeconds(5))
                .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                .build();

        connection.httpClient().sendAsync(request, HttpResponse.BodyHandlers.discarding())
                .exceptionally(ignored -> null);
    }

    private static String toClientOutcome(LicenseOutcome outcome) {
        switch (outcome) {
            case SIGNATURE_INVALID:
                return "SIGNATURE_INVALID";
            case RESPONSE_INVALID:
                return "RESPONSE_INVALID";
            case NONCE_INVALID:
                return "NONCE_INVALID";
            default:
                return null;
        }
    }

    String buildRequestBody(Map<String, Object> fields, String nonce) {
        Map<String, Object> payload = new LinkedHashMap<>(fields);
        payload.put("nonce", nonce);
        return SimpleJson.object(payload);
    }

    private static String encodePathSegment(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8).replace("+", "%20");
    }
}
