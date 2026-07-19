package net.opmasterleo.license.internal.http;

public final class HttpExchange {

    private final boolean networkFailure;
    private final int statusCode;
    private final String body;
    private final String signature;
    private final String signatureAlgorithm;
    private final String errorMessage;

    private HttpExchange(
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

    public static HttpExchange success(int statusCode, String body, String signature, String signatureAlgorithm) {
        return new HttpExchange(false, statusCode, body, signature, signatureAlgorithm, null);
    }

    public static HttpExchange networkFailure(String errorMessage) {
        String message = errorMessage;
        if (message == null || message.isEmpty()) {
            message = "Network request failed";
        }
        return new HttpExchange(true, -1, null, null, null, message);
    }

    public boolean networkFailure() {
        return networkFailure;
    }

    public int statusCode() {
        return statusCode;
    }

    public String body() {
        return body;
    }

    public String signature() {
        return signature;
    }

    public String signatureAlgorithm() {
        return signatureAlgorithm;
    }

    public String errorMessage() {
        return errorMessage;
    }
}
