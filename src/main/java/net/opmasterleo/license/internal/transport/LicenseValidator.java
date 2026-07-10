package net.opmasterleo.license.internal.transport;

import net.opmasterleo.license.exception.LicenseException;
import net.opmasterleo.license.internal.runtime.LicenseRuntime;
import net.opmasterleo.license.model.LicenseEnvironment;
import net.opmasterleo.license.model.LicenseOutcome;
import net.opmasterleo.license.model.LicenseResult;

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

        LicenseHttpResponse response = transport.post(connection, requestBody);
        if (response.networkFailure()) {
            return parser.failure(
                    LicenseOutcome.NETWORK_ERROR,
                    connection.product(),
                    environment,
                    runtime,
                    null,
                    new LicenseException(response.errorMessage())
            );
        }

        String responseBody = response.body();
        int statusCode = response.statusCode();
        if (statusCode >= 500) {
            return parser.failure(
                    LicenseOutcome.NETWORK_ERROR,
                    connection.product(),
                    environment,
                    runtime,
                    responseBody,
                    new LicenseException("OPLicense API returned HTTP " + statusCode)
            );
        }

        String signature = response.signature();
        if (signature != null) {
            signature = signature.trim();
        }

        return parser.parseVerifiedResponse(
                connection,
                runtime,
                environment,
                requestNonce,
                responseBody,
                signature,
                response.signatureAlgorithm()
        );
    }

    private static String randomNonce() {
        byte[] raw = new byte[24];
        RANDOM.nextBytes(raw);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(raw);
    }
}
