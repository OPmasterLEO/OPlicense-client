package net.opmasterleo.license.internal.http;

import net.opmasterleo.license.internal.util.Io;
import net.opmasterleo.license.internal.util.Threads;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

public final class LicenseHttp {

    private static final int CONNECT_TIMEOUT_MS = 10_000;
    private static final int READ_TIMEOUT_MS = 15_000;
    private static final int MAX_ATTEMPTS = 3;
    private static final int MAX_BODY_BYTES = 1_048_576;

    public LicenseHttp() {
    }

    public HttpExchange post(String url, String requestBody, String userAgent) {
        HttpExchange lastFailure = null;
        for (int attempt = 1; attempt <= MAX_ATTEMPTS; attempt++) {
            HttpExchange response = executeOnce(url, requestBody, userAgent, CONNECT_TIMEOUT_MS, READ_TIMEOUT_MS);
            if (!response.networkFailure()) {
                return response;
            }
            lastFailure = response;
            if (attempt < MAX_ATTEMPTS) {
                Threads.sleepQuietly(500L * (long) attempt);
            }
        }
        if (lastFailure != null) {
            return lastFailure;
        }
        return HttpExchange.networkFailure("No response received");
    }

    public static HttpExchange executeOnce(
            String url,
            String requestBody,
            String userAgent,
            int connectTimeoutMs,
            int readTimeoutMs
    ) {
        HttpURLConnection connection = null;
        try {
            connection = openPost(url, userAgent, connectTimeoutMs, readTimeoutMs);
            writeBody(connection, requestBody);
            int statusCode = connection.getResponseCode();
            String body = readBody(connection, statusCode);
            String signature = header(connection, "x-signature", "X-Signature");
            String algorithm = header(connection, "x-signature-alg", "X-Signature-Alg");
            return HttpExchange.success(statusCode, body, signature, algorithm);
        } catch (Exception e) {
            String message = e.getMessage();
            if (message == null || message.isEmpty()) {
                message = e.getClass().getSimpleName();
            }
            return HttpExchange.networkFailure(message);
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    private static HttpURLConnection openPost(
            String url,
            String userAgent,
            int connectTimeoutMs,
            int readTimeoutMs
    ) throws Exception {
        HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
        connection.setRequestMethod("POST");
        connection.setRequestProperty("Content-Type", "application/json; charset=utf-8");
        connection.setRequestProperty("Accept", "application/json");
        if (userAgent != null && !userAgent.isEmpty()) {
            connection.setRequestProperty("User-Agent", userAgent);
        }
        connection.setDoOutput(true);
        connection.setInstanceFollowRedirects(false);
        connection.setConnectTimeout(connectTimeoutMs);
        connection.setReadTimeout(readTimeoutMs);
        return connection;
    }

    private static void writeBody(HttpURLConnection connection, String requestBody) throws Exception {
        byte[] bytes = requestBody.getBytes(StandardCharsets.UTF_8);
        OutputStream outputStream = connection.getOutputStream();
        try {
            outputStream.write(bytes, 0, bytes.length);
            outputStream.flush();
        } finally {
            Io.closeQuietly(outputStream);
        }
    }

    private static String readBody(HttpURLConnection connection, int statusCode) throws Exception {
        InputStream stream;
        if (statusCode >= 400) {
            stream = connection.getErrorStream();
        } else {
            stream = connection.getInputStream();
        }
        if (stream == null) {
            return "";
        }
        try {
            return Io.readLimitedUtf8(stream, MAX_BODY_BYTES);
        } finally {
            Io.closeQuietly(stream);
        }
    }

    private static String header(HttpURLConnection connection, String primary, String alternate) {
        String value = connection.getHeaderField(primary);
        if (value == null || value.isEmpty()) {
            value = connection.getHeaderField(alternate);
        }
        return value;
    }
}
