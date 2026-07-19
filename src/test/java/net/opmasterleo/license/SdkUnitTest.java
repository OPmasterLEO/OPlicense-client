package net.opmasterleo.license;

import net.opmasterleo.license.internal.json.Json;
import net.opmasterleo.license.internal.security.ResponseGuard;
import net.opmasterleo.license.model.LicenseOutcome;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SdkUnitTest {

    @Test
    void jsonRoundTripEncodesAndParsesScalars() {
        Map<String, Object> fields = new LinkedHashMap<>();
        fields.put("nonce", "abc_123-XYZ");
        fields.put("timestamp", 1700000000L);
        fields.put("valid", true);
        String json = Json.object(fields);

        Map<String, String> parsed = Json.parseFlat(json);
        assertEquals("abc_123-XYZ", parsed.get("nonce"));
        assertEquals("1700000000", parsed.get("timestamp"));
        assertEquals("true", parsed.get("valid"));
    }

    @Test
    void mapReasonCoversServerCodes() throws Exception {
        Method method = ResponseGuard.class.getDeclaredMethod("mapReason", String.class);
        method.setAccessible(true);

        assertEquals(LicenseOutcome.PRODUCT_MISMATCH, method.invoke(null, "PRODUCT_NOT_FOUND"));
        assertEquals(LicenseOutcome.MAX_HWIDS_REACHED, method.invoke(null, "MAX_HWIDS_REACHED"));
        assertEquals(LicenseOutcome.RATE_LIMITED, method.invoke(null, "RATE_LIMITED"));
        assertEquals(LicenseOutcome.NETWORK_ERROR, method.invoke(null, "UNKNOWN_CODE"));
    }

    @Test
    void versionIsTwoZero() {
        assertTrue(LicenseClient.VERSION.startsWith("2."));
    }
}
