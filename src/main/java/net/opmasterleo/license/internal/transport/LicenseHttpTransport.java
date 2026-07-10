package net.opmasterleo.license.internal.transport;

import net.opmasterleo.license.internal.json.SimpleJson;
import net.opmasterleo.license.internal.platform.PlatformSupport;
import net.opmasterleo.license.internal.runtime.LicenseRuntime;
import net.opmasterleo.license.model.LicenseOutcome;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;

final class LicenseHttpTransport {

    private static final int CONNECT_TIMEOUT_MS = 10000;
    private static final int READ_TIMEOUT_MS = 15000;
    private static final int OUTCOME_CONNECT_TIMEOUT_MS = 5000;
    private static final int OUTCOME_READ_TIMEOUT_MS = 5000;

    LicenseHttpResponse post(LicenseConnection connection, String requestBody) throws IOException, InterruptedException {
        String url = connection.apiUrl()
                + "/v1/license/"
                + encodePathSegment(connection.product())
                + "/"
                + encodePathSegment(connection.licenseKey());

        IOException lastIo = null;
        int maxAttempts = 3;
        long backoffBaseMs = 500;
        LicenseHttpResponse response = null;

        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            try {
                response = executePost(url, requestBody, CONNECT_TIMEOUT_MS, READ_TIMEOUT_MS);
                break;
            } catch (IOException e) {
                lastIo = e;
                if (attempt < maxAttempts) {
                    PlatformSupport.sleep(backoffBaseMs * attempt);
                }
            }
        }

        if (response == null) {
            throw lastIo != null ? lastIo : new IOException("No response received");
        }
        return response;
    }

    void reportClientOutcome(LicenseConnection connection, LicenseRuntime runtime, LicenseOutcome outcome) {
        String outcomeCode = toClientOutcome(outcome);
        if (outcomeCode == null) {
            return;
        }

        Map<String, Object> fields = new LinkedHashMap<>(runtime.toRequestFields());
        fields.put("outcome", outcomeCode);
        String requestBody = SimpleJson.object(fields);

        String url = connection.apiUrl()
                + "/v1/license/"
                + encodePathSegment(connection.product())
                + "/"
                + encodePathSegment(connection.licenseKey())
                + "/client-outcome";

        new OutcomeReportThread(url, requestBody).start();
    }

    String buildRequestBody(Map<String, Object> fields, String nonce) {
        Map<String, Object> payload = new LinkedHashMap<>(fields);
        payload.put("nonce", nonce);
        return SimpleJson.object(payload);
    }

    private static LicenseHttpResponse executePost(String url, String requestBody, int connectTimeoutMs, int readTimeoutMs)
            throws IOException {
        HttpURLConnection conn = openJsonPost(url, connectTimeoutMs, readTimeoutMs);
        writeBody(conn, requestBody);

        int statusCode = conn.getResponseCode();
        String body = readBody(conn, statusCode);
        String signature = firstHeader(conn, "x-signature", "X-Signature");
        String algorithm = firstHeader(conn, "x-signature-alg", "X-Signature-Alg");
        conn.disconnect();
        return new LicenseHttpResponse(statusCode, body, signature, algorithm);
    }

    static void executePostFireAndForget(String url, String requestBody, int connectTimeoutMs, int readTimeoutMs) {
        HttpURLConnection conn = null;
        try {
            conn = openJsonPost(url, connectTimeoutMs, readTimeoutMs);
            writeBody(conn, requestBody);
            int statusCode = conn.getResponseCode();
            readBody(conn, statusCode);
        } catch (Exception ignored) {
        } finally {
            if (conn != null) {
                conn.disconnect();
            }
        }
    }

    private static HttpURLConnection openJsonPost(String url, int connectTimeoutMs, int readTimeoutMs) throws IOException {
        HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "application/json; charset=utf-8");
        conn.setRequestProperty("Accept", "application/json");
        conn.setDoOutput(true);
        conn.setConnectTimeout(connectTimeoutMs);
        conn.setReadTimeout(readTimeoutMs);
        return conn;
    }

    private static void writeBody(HttpURLConnection conn, String requestBody) throws IOException {
        OutputStream outputStream = conn.getOutputStream();
        byte[] bytes = requestBody.getBytes(StandardCharsets.UTF_8);
        outputStream.write(bytes, 0, bytes.length);
        outputStream.close();
    }

    private static String readBody(HttpURLConnection conn, int statusCode) throws IOException {
        InputStream stream = statusCode >= 400 ? conn.getErrorStream() : conn.getInputStream();
        if (stream == null) {
            return "";
        }

        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        byte[] chunk = new byte[4096];
        int read;
        while ((read = stream.read(chunk)) != -1) {
            buffer.write(chunk, 0, read);
        }
        stream.close();
        return buffer.toString(StandardCharsets.UTF_8.name());
    }

    private static String firstHeader(HttpURLConnection conn, String primary, String alternate) {
        String value = conn.getHeaderField(primary);
        if (value == null || value.isEmpty()) {
            value = conn.getHeaderField(alternate);
        }
        return value;
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

    private static String encodePathSegment(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8).replace("+", "%20");
    }

    static int outcomeConnectTimeoutMs() {
        return OUTCOME_CONNECT_TIMEOUT_MS;
    }

    static int outcomeReadTimeoutMs() {
        return OUTCOME_READ_TIMEOUT_MS;
    }
}
