package net.opmasterleo.license.internal.transport;

import net.opmasterleo.license.internal.json.SimpleJson;
import net.opmasterleo.license.internal.platform.PlatformSupport;
import net.opmasterleo.license.internal.runtime.LicenseRuntime;
import net.opmasterleo.license.model.LicenseOutcome;

import java.io.ByteArrayOutputStream;
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

    LicenseHttpResponse post(LicenseConnection connection, String requestBody) {
        String url = connection.apiUrl()
                + "/v1/license/"
                + encodePath(connection.product())
                + "/"
                + encodePath(connection.licenseKey());

        LicenseHttpResponse last = null;
        for (int attempt = 1; attempt <= 3; attempt++) {
            LicenseHttpResponse response = executeOnce(url, requestBody, CONNECT_TIMEOUT_MS, READ_TIMEOUT_MS);
            if (!response.networkFailure()) {
                return response;
            }
            last = response;
            if (attempt < 3) {
                PlatformSupport.sleep(500L * attempt);
            }
        }
        if (last != null) {
            return last;
        }
        return LicenseHttpResponse.networkFailure("No response received");
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
                + encodePath(connection.product())
                + "/"
                + encodePath(connection.licenseKey())
                + "/client-outcome";

        new OutcomeReportThread(url, requestBody).start();
    }

    String buildRequestBody(Map<String, Object> fields, String nonce) {
        Map<String, Object> payload = new LinkedHashMap<>(fields);
        payload.put("nonce", nonce);
        return SimpleJson.object(payload);
    }

    static LicenseHttpResponse executeOnce(String url, String requestBody, int connectTimeoutMs, int readTimeoutMs) {
        HttpURLConnection conn = null;
        try {
            conn = openPost(url, connectTimeoutMs, readTimeoutMs);
            writeBody(conn, requestBody);
            int statusCode = conn.getResponseCode();
            String body = readBody(conn, statusCode);
            String signature = header(conn, "x-signature", "X-Signature");
            String algorithm = header(conn, "x-signature-alg", "X-Signature-Alg");
            conn.disconnect();
            return LicenseHttpResponse.success(statusCode, body, signature, algorithm);
        } catch (Exception e) {
            if (conn != null) {
                conn.disconnect();
            }
            return LicenseHttpResponse.networkFailure(e.getMessage());
        }
    }

    private static HttpURLConnection openPost(String url, int connectTimeoutMs, int readTimeoutMs) throws Exception {
        HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "application/json; charset=utf-8");
        conn.setRequestProperty("Accept", "application/json");
        conn.setDoOutput(true);
        conn.setConnectTimeout(connectTimeoutMs);
        conn.setReadTimeout(readTimeoutMs);
        return conn;
    }

    private static void writeBody(HttpURLConnection conn, String requestBody) throws Exception {
        OutputStream outputStream = conn.getOutputStream();
        byte[] bytes = requestBody.getBytes(StandardCharsets.UTF_8);
        outputStream.write(bytes, 0, bytes.length);
        outputStream.close();
    }

    private static String readBody(HttpURLConnection conn, int statusCode) throws Exception {
        InputStream stream = statusCode >= 400 ? conn.getErrorStream() : conn.getInputStream();
        if (stream == null) {
            return "";
        }
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        byte[] chunk = new byte[4096];
        int read = stream.read(chunk);
        while (read != -1) {
            buffer.write(chunk, 0, read);
            read = stream.read(chunk);
        }
        stream.close();
        return buffer.toString(StandardCharsets.UTF_8.name());
    }

    private static String header(HttpURLConnection conn, String primary, String alternate) {
        String value = conn.getHeaderField(primary);
        if (value == null || value.isEmpty()) {
            value = conn.getHeaderField(alternate);
        }
        return value;
    }

    private static String toClientOutcome(LicenseOutcome outcome) {
        if (outcome == LicenseOutcome.SIGNATURE_INVALID) {
            return "SIGNATURE_INVALID";
        }
        if (outcome == LicenseOutcome.RESPONSE_INVALID) {
            return "RESPONSE_INVALID";
        }
        if (outcome == LicenseOutcome.NONCE_INVALID) {
            return "NONCE_INVALID";
        }
        return null;
    }

    private static String encodePath(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8).replace("+", "%20");
    }
}

final class LicenseHttpResponse {

    private final boolean networkFailure;
    private final int statusCode;
    private final String body;
    private final String signature;
    private final String signatureAlgorithm;
    private final String errorMessage;

    private LicenseHttpResponse(
            boolean networkFailure,
            int statusCode,
            String body,
            String signature,
            String signatureAlgorithm,
            String errorMessage
    ) {
        this.networkFailure = networkFailure;
        this.statusCode = statusCode;
        this.body = body;
        this.signature = signature;
        this.signatureAlgorithm = signatureAlgorithm;
        this.errorMessage = errorMessage;
    }

    static LicenseHttpResponse success(int statusCode, String body, String signature, String signatureAlgorithm) {
        return new LicenseHttpResponse(false, statusCode, body, signature, signatureAlgorithm, null);
    }

    static LicenseHttpResponse networkFailure(String errorMessage) {
        String message = errorMessage;
        if (message == null || message.isEmpty()) {
            message = "Network request failed";
        }
        return new LicenseHttpResponse(true, -1, null, null, null, message);
    }

    boolean networkFailure() {
        return networkFailure;
    }

    int statusCode() {
        return statusCode;
    }

    String body() {
        return body;
    }

    String signature() {
        return signature;
    }

    String signatureAlgorithm() {
        return signatureAlgorithm;
    }

    String errorMessage() {
        return errorMessage;
    }
}

final class OutcomeReportThread extends Thread {

    private final String url;
    private final String requestBody;

    OutcomeReportThread(String url, String requestBody) {
        this.url = url;
        this.requestBody = requestBody;
        setDaemon(true);
        setName("oplicense-outcome-report");
    }

    @Override
    public void run() {
        LicenseHttpTransport.executeOnce(url, requestBody, 5000, 5000);
    }
}
