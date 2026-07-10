package net.opmasterleo.license.api;

import org.junit.jupiter.api.Test;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.util.Base64;

import static org.junit.jupiter.api.Assertions.assertFalse;

class ResponseVerifierSignatureContractTest {

    @Test
    void verifyMethodAcceptsThreeStringArguments() throws Exception {
        KeyPairGenerator generator = KeyPairGenerator.getInstance("Ed25519");
        KeyPair pair = generator.generateKeyPair();
        String publicSpki = Base64.getEncoder().encodeToString(pair.getPublic().getEncoded());

        Ed25519ResponseVerifier verifier = Ed25519ResponseVerifier.createEd25519(publicSpki);
        assertFalse(verifier.verify("{\"valid\":false}", "invalid", "ed25519"));
    }
}
