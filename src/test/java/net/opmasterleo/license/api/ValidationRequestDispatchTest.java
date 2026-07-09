package net.opmasterleo.license.api;

import net.opmasterleo.license.exception.LicenseException;
import net.opmasterleo.license.model.LicenseOutcome;
import net.opmasterleo.license.model.LicenseResult;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ValidationRequestDispatchTest {

    @Test
    void dispatchInvokesValidCallback() throws Exception {
        ValidationRequest request = new ValidationRequest(null);
        AtomicBoolean called = new AtomicBoolean(false);
        request.onValid(() -> called.set(true));

        invokeDispatch(request, new LicenseResult(LicenseOutcome.VALID, "prod", "ACTIVE", null, "{}", null));

        assertTrue(called.get());
    }

    @Test
    void dispatchRoutesSignatureLikeFailuresToOnSignatureInvalid() throws Exception {
        ValidationRequest request = new ValidationRequest(null);
        AtomicReference<LicenseOutcome> captured = new AtomicReference<>();
        request.onSignatureInvalid(result -> captured.set(result.outcome()));

        invokeDispatch(request, new LicenseResult(LicenseOutcome.NONCE_INVALID, "prod", null, null, "{}", null));
        assertEquals(LicenseOutcome.NONCE_INVALID, captured.get());

        invokeDispatch(request, new LicenseResult(LicenseOutcome.RESPONSE_INVALID, "prod", null, null, "{}", null));
        assertEquals(LicenseOutcome.RESPONSE_INVALID, captured.get());
    }

    @Test
    void dispatchInvokesNetworkErrorCallback() throws Exception {
        ValidationRequest request = new ValidationRequest(null);
        AtomicBoolean called = new AtomicBoolean(false);
        request.onNetworkError(err -> called.set(true));

        invokeDispatch(request, new LicenseResult(
                LicenseOutcome.NETWORK_ERROR,
                "prod",
                null,
                null,
                null,
                new LicenseException("network down")
        ));

        assertTrue(called.get());
    }

    @Test
    void dispatchDoesNothingWithoutRegisteredCallback() throws Exception {
        ValidationRequest request = new ValidationRequest(null);
        AtomicBoolean called = new AtomicBoolean(false);
        request.onRateLimited(result -> called.set(true));

        invokeDispatch(request, new LicenseResult(LicenseOutcome.EXPIRED, "prod", "EXPIRED", null, "{}", null));

        assertFalse(called.get());
    }

    private static void invokeDispatch(ValidationRequest request, LicenseResult result) throws Exception {
        Method dispatch = ValidationRequest.class.getDeclaredMethod("dispatch", LicenseResult.class);
        dispatch.setAccessible(true);
        dispatch.invoke(request, result);
    }
}
