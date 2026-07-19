package net.opmasterleo.license.internal.core;

import net.opmasterleo.license.exception.LicenseException;
import net.opmasterleo.license.internal.http.HttpExchange;
import net.opmasterleo.license.internal.http.LicenseHttp;
import net.opmasterleo.license.internal.http.OutcomeReporter;
import net.opmasterleo.license.internal.json.Json;
import net.opmasterleo.license.internal.security.Nonce;
import net.opmasterleo.license.internal.security.ResponseGuard;
import net.opmasterleo.license.internal.util.Strings;
import net.opmasterleo.license.model.LicenseEnvironment;
import net.opmasterleo.license.model.LicenseOutcome;
import net.opmasterleo.license.model.LicenseResult;
import net.opmasterleo.license.model.LicenseUpdate;

import java.util.LinkedHashMap;
import java.util.Map;

public final class ValidationEngine {

    private final LicenseHttp http = new LicenseHttp();
    private final ResponseGuard guard = new ResponseGuard();
    private final OutcomeReporter reporter = new OutcomeReporter();

    public ValidationEngine() {
    }

    public LicenseResult validate(ClientConfig config, RequestContext context) {
        LicenseEnvironment environment = context.environment();
        String requestNonce = Nonce.create();
        Map<String, Object> fields = new LinkedHashMap<>(context.toRequestFields());
        fields.put("nonce", requestNonce);
        String requestBody = Json.object(fields);

        HttpExchange response = http.post(config.validateUrl(), requestBody, config.userAgent());
        if (response.networkFailure()) {
            return networkFailure(config.product(), environment, context, response.errorMessage(), null);
        }

        int statusCode = response.statusCode();
        if (statusCode >= 500) {
            return networkFailure(
                    config.product(),
                    environment,
                    context,
                    "OPLicense API returned HTTP " + statusCode,
                    response.body()
            );
        }

        LicenseResult verified = guard.verifyAndParse(
                config,
                context,
                environment,
                requestNonce,
                response.body(),
                response.signature(),
                response.signatureAlgorithm()
        );

        if (shouldReport(verified.outcome())) {
            reporter.report(config, context, verified.outcome());
        }
        return verified;
    }

    private static boolean shouldReport(LicenseOutcome outcome) {
        return outcome == LicenseOutcome.SIGNATURE_INVALID
                || outcome == LicenseOutcome.RESPONSE_INVALID
                || outcome == LicenseOutcome.NONCE_INVALID;
    }

    private static LicenseResult networkFailure(
            String product,
            LicenseEnvironment environment,
            RequestContext context,
            String message,
            String rawBody
    ) {
        String resolved = Strings.firstNonBlank(message, defaultNetworkMessage());
        return LicenseResult.create(
                LicenseOutcome.NETWORK_ERROR,
                product,
                null,
                null,
                null,
                null,
                null,
                null,
                environment,
                LicenseUpdate.none(context.productVersion()),
                rawBody,
                new LicenseException(resolved)
        );
    }

    private static String defaultNetworkMessage() {
        return "Could not reach the OPLicense API. The license server may be offline or unreachable.";
    }
}
