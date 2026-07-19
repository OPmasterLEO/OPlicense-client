package net.opmasterleo.license.internal.http;

import net.opmasterleo.license.internal.core.ClientConfig;
import net.opmasterleo.license.internal.core.RequestContext;
import net.opmasterleo.license.internal.json.Json;
import net.opmasterleo.license.model.LicenseOutcome;

import java.util.LinkedHashMap;
import java.util.Map;

public final class OutcomeReporter {

    public OutcomeReporter() {
    }

    public void report(ClientConfig config, RequestContext context, LicenseOutcome outcome) {
        String outcomeCode = toClientOutcome(outcome);
        if (outcomeCode == null) {
            return;
        }
        Map<String, Object> fields = new LinkedHashMap<>(context.toRequestFields());
        fields.put("outcome", outcomeCode);
        String requestBody = Json.object(fields);
        OutcomeReportThread thread = new OutcomeReportThread(
                config.clientOutcomeUrl(),
                requestBody,
                config.userAgent()
        );
        thread.start();
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
}
