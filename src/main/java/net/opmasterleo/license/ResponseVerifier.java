package net.opmasterleo.license;

import net.opmasterleo.license.internal.Ed25519;
import net.opmasterleo.license.internal.Hmac;

import java.security.PublicKey;

/**
 * Verifies signed API responses. Prefer {@link #ed25519(String)} in production plugins —
 * only the public key ships in the JAR, so a leaked client cannot forge valid responses.
 */
public interface ResponseVerifier {

    boolean verify(String payload, String signature, String algorithmHeader);

    static ResponseVerifier hmac(String secret) {
        if (secret == null || secret.isEmpty()) {
            throw new LicenseException("HMAC secret is required");
        }
        return new HmacResponseVerifier(secret);
    }

    static ResponseVerifier ed25519(String spkiBase64) {
        PublicKey publicKey = Ed25519.decodePublicKey(spkiBase64);
        return new Ed25519ResponseVerifier(publicKey);
    }

    static String normalizeAlgorithm(String algorithmHeader) {
        if (algorithmHeader == null || algorithmHeader.isBlank()) {
            return "hmac";
        }
        return algorithmHeader.trim().toLowerCase();
    }

    final class HmacResponseVerifier implements ResponseVerifier {
        private final String secret;

        HmacResponseVerifier(String secret) {
            this.secret = secret;
        }

        @Override
        public boolean verify(String payload, String signature, String algorithmHeader) {
            String algorithm = normalizeAlgorithm(algorithmHeader);
            if (!"hmac".equals(algorithm)) {
                return false;
            }
            return Hmac.verify(payload, secret, signature);
        }
    }

    final class Ed25519ResponseVerifier implements ResponseVerifier {
        private final PublicKey publicKey;

        Ed25519ResponseVerifier(PublicKey publicKey) {
            this.publicKey = publicKey;
        }

        @Override
        public boolean verify(String payload, String signature, String algorithmHeader) {
            String algorithm = normalizeAlgorithm(algorithmHeader);
            if (!"ed25519".equals(algorithm)) {
                return false;
            }
            return Ed25519.verify(payload, signature, publicKey);
        }
    }
}
