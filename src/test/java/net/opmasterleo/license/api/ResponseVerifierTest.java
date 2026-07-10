package net.opmasterleo.license.api;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.Signature;
import java.util.Base64;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ResponseVerifierTest {

    @Test
    void ed25519VerifierAcceptsValidPayloadAndSignature() throws Exception {
        KeyPairGenerator generator = KeyPairGenerator.getInstance("Ed25519");
        KeyPair pair = generator.generateKeyPair();

        String payload = "{\"valid\":true,\"status\":\"ACTIVE\"}";
        Signature signer = Signature.getInstance("Ed25519");
        signer.initSign(pair.getPrivate());
        signer.update(payload.getBytes(StandardCharsets.UTF_8));
        String signature = Base64.getEncoder().encodeToString(signer.sign());

        String publicSpki = Base64.getEncoder().encodeToString(pair.getPublic().getEncoded());
        Ed25519ResponseVerifier verifier = ResponseVerifiers.createEd25519(publicSpki);

        assertTrue(verifier.verify(payload, signature, "ed25519"));
    }

    @Test
    void ed25519VerifierAcceptsMissingAlgorithmHeader() throws Exception {
        KeyPairGenerator generator = KeyPairGenerator.getInstance("Ed25519");
        KeyPair pair = generator.generateKeyPair();

        String payload = "{\"valid\":true}";
        Signature signer = Signature.getInstance("Ed25519");
        signer.initSign(pair.getPrivate());
        signer.update(payload.getBytes(StandardCharsets.UTF_8));
        String signature = Base64.getEncoder().encodeToString(signer.sign());

        String publicSpki = Base64.getEncoder().encodeToString(pair.getPublic().getEncoded());
        Ed25519ResponseVerifier verifier = ResponseVerifiers.createEd25519(publicSpki);

        assertTrue(verifier.verify(payload, signature, null));
    }

    @Test
    void ed25519VerifierRejectsUnsupportedAlgorithmHeader() throws Exception {
        KeyPairGenerator generator = KeyPairGenerator.getInstance("Ed25519");
        KeyPair pair = generator.generateKeyPair();
        String publicSpki = Base64.getEncoder().encodeToString(pair.getPublic().getEncoded());
        Ed25519ResponseVerifier verifier = ResponseVerifiers.createEd25519(publicSpki);

        assertFalse(verifier.verify("{\"valid\":true}", "invalid", "hmac"));
    }
}
