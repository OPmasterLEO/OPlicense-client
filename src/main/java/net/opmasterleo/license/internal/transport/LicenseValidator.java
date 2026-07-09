package net.opmasterleo.license.internal.transport;

import net.opmasterleo.license.model.LicenseEnvironment;
import net.opmasterleo.license.model.LicenseOutcome;
import net.opmasterleo.license.model.LicenseResult;
import net.opmasterleo.license.internal.runtime.LicenseRuntime;

import java.io.IOException;
import java.net.http.HttpResponse;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.Map;

public final class LicenseValidator {

    private static final SecureRandom RANDOM = new SecureRandom();

    private final LicenseHttpTransport transport = new LicenseHttpTransport();
    private final LicenseResponseParser parser = new LicenseResponseParser();

    public LicenseValidator() {
    }

    public LicenseResult validate(LicenseConnection connection, LicenseRuntime runtime) {
        LicenseEnvironment environment = runtime.environment();
        String requestNonce = randomNonce();
        Map<String, Object> fields = runtime.toRequestFields();
        String requestBody = transport.buildRequestBody(fields, requestNonce);

        try {
            HttpResponse<String> response = transport.post(connection, requestBody);
            String responseBody = response.body();
            int statusCode = response.statusCode();

            if (statusCode >= 500) {
                return parser.failure(LicenseOutcome.NETWORK_ERROR, connection.product(), environment, runtime, responseBody, null);
            }

            String signature = response.headers().firstValue("x-signature")
                    .or(() -> response.headers().firstValue("X-Signature"))
                    .orElse(null);
            if (signature != null) signature = signature.trim();

            String algorithm = response.headers().firstValue("x-signature-alg")
                    .or(() -> response.headers().firstValue("X-Signature-Alg"))
                    .orElse(null);

            return parser.parseVerifiedResponse(
                    connection,
                    runtime,
                    environment,
                    requestNonce,
                    responseBody,
                    signature,
                    algorithm
            );
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return parser.failure(LicenseOutcome.NETWORK_ERROR, connection.product(), environment, runtime, null, e);
        } catch (IOException e) {
            return parser.failure(LicenseOutcome.NETWORK_ERROR, connection.product(), environment, runtime, null, e);
        }
    }

    private static String randomNonce() {
        byte[] raw = new byte[24];
        RANDOM.nextBytes(raw);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(raw);
    }
}
