package net.opmasterleo.license.internal.transport;

import net.opmasterleo.license.api.Ed25519ResponseVerifier;
import org.junit.jupiter.api.Test;

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
                Ed25519ResponseVerifier.createEd25519(generateEd25519Spki())
        );

        assertEquals("https://license.mastersmp.net", connection.apiUrl());
    }

    @Test
    void rejectsBlankApiUrlAfterNormalization() throws Exception {
        assertThrows(IllegalArgumentException.class, () -> new LicenseConnection(
                "   ///   ",
                "key",
                "product",
                Ed25519ResponseVerifier.createEd25519(generateEd25519Spki())
        ));
    }

    private static String generateEd25519Spki() throws Exception {
        KeyPairGenerator generator = KeyPairGenerator.getInstance("Ed25519");
        KeyPair pair = generator.generateKeyPair();
        return Base64.getEncoder().encodeToString(pair.getPublic().getEncoded());
    }
}
