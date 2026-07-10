package net.opmasterleo.license.api;

import net.opmasterleo.license.exception.LicenseException;
import net.opmasterleo.license.model.LicenseOutcome;
import net.opmasterleo.license.model.LicenseResult;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ValidationRequestDispatchTest {

    @Test
    void dispatchInvokesValidCallback() {
        ValidationRequest request = new ValidationRequest(null);
        AtomicBoolean called = new AtomicBoolean(false);
        ValidationCallbacks callbacks = new ValidationCallbacks() {
            @Override
            public void onValid(LicenseResult result) {
                called.set(true);
            }
        };

        request.dispatch(new LicenseResult(LicenseOutcome.VALID, "prod", "ACTIVE", null, "{}", null), callbacks);

        assertTrue(called.get());
    }

    @Test
    void dispatchRoutesSignatureLikeFailuresToOnSignatureInvalid() {
        ValidationRequest request = new ValidationRequest(null);
        AtomicReference<LicenseOutcome> captured = new AtomicReference<>();
        ValidationCallbacks callbacks = new ValidationCallbacks() {
            @Override
            public void onSignatureInvalid(LicenseResult result) {
                captured.set(result.outcome());
            }
        };

        request.dispatch(new LicenseResult(LicenseOutcome.NONCE_INVALID, "prod", null, null, "{}", null), callbacks);
        assertEquals(LicenseOutcome.NONCE_INVALID, captured.get());

        request.dispatch(new LicenseResult(LicenseOutcome.RESPONSE_INVALID, "prod", null, null, "{}", null), callbacks);
        assertEquals(LicenseOutcome.RESPONSE_INVALID, captured.get());
    }

    @Test
    void dispatchInvokesNetworkErrorCallback() {
        ValidationRequest request = new ValidationRequest(null);
        AtomicBoolean called = new AtomicBoolean(false);
        ValidationCallbacks callbacks = new ValidationCallbacks() {
            @Override
            public void onNetworkError(Exception error) {
                called.set(true);
            }
        };

        request.dispatch(new LicenseResult(
                LicenseOutcome.NETWORK_ERROR,
                "prod",
                null,
                null,
                null,
                new LicenseException("network down")
        ), callbacks);

        assertTrue(called.get());
    }

    @Test
    void dispatchDoesNothingWithoutRegisteredCallback() {
        ValidationRequest request = new ValidationRequest(null);
        AtomicBoolean called = new AtomicBoolean(false);
        ValidationCallbacks callbacks = new ValidationCallbacks() {
            @Override
            public void onRateLimited(LicenseResult result) {
                called.set(true);
            }
        };

        request.dispatch(new LicenseResult(LicenseOutcome.EXPIRED, "prod", "EXPIRED", null, "{}", null), callbacks);

        assertFalse(called.get());
    }

    @Test
    void dispatchProvidesMessageWhenNetworkErrorExceptionIsMissing() {
        ValidationRequest request = new ValidationRequest(null);
        AtomicReference<String> captured = new AtomicReference<>();
        ValidationCallbacks callbacks = new ValidationCallbacks() {
            @Override
            public void onNetworkError(Exception error) {
                captured.set(error != null ? error.getMessage() : null);
            }
        };

        request.dispatch(new LicenseResult(LicenseOutcome.NETWORK_ERROR, "prod", null, null, "{}", null), callbacks);

        assertEquals(
                "Could not reach the OPLicense API. The license server may be offline or unreachable.",
                captured.get()
        );
    }
}
