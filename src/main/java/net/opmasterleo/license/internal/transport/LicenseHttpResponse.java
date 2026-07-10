package net.opmasterleo.license.internal.transport;

final class LicenseHttpResponse {

    private final int statusCode;
    private final String body;
    private final String signature;
    private final String signatureAlgorithm;

    LicenseHttpResponse(int statusCode, String body, String signature, String signatureAlgorithm) {
        this.statusCode = statusCode;
        this.body = body;
        this.signature = signature;
        this.signatureAlgorithm = signatureAlgorithm;
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
}
