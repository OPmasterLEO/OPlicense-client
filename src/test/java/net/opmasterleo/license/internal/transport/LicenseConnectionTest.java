package net.opmasterleo.license.internal.transport;

import net.opmasterleo.license.api.ResponseVerifier;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.util.Base64;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class LicenseConnectionTest {

    @Test
    void stripsTrailingSlashesFromApiUrl() throws Exception {
        LicenseConnection connection = new LicenseConnection(
                "https://license.mastersmp.net///",
                "key",
                "product",
                ResponseVerifier.ed25519(generateEd25519Spki())
        );

        assertEquals("https://license.mastersmp.net", readApiUrl(connection));
    }

    @Test
    void rejectsBlankApiUrlAfterNormalization() throws Exception {
        assertThrows(IllegalArgumentException.class, () -> new LicenseConnection(
                "   ///   ",
                "key",
                "product",
                ResponseVerifier.ed25519(generateEd25519Spki())
        ));
    }

    private static String readApiUrl(LicenseConnection connection) throws Exception {
        Method method = LicenseConnection.class.getDeclaredMethod("apiUrl");
        method.setAccessible(true);
        return (String) method.invoke(connection);
    }

    private static String generateEd25519Spki() throws Exception {
        KeyPairGenerator generator = KeyPairGenerator.getInstance("Ed25519");
        KeyPair pair = generator.generateKeyPair();
        return Base64.getEncoder().encodeToString(pair.getPublic().getEncoded());
    }
}
