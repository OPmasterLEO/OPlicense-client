package net.opmasterleo.license.api;

import net.opmasterleo.license.internal.crypto.Ed25519;

import java.security.PublicKey;

public interface ResponseVerifier {

    boolean verify(String payload, String signature, String algorithmHeader);

    static ResponseVerifier ed25519(String spkiBase64) {
        PublicKey publicKey = Ed25519.decodePublicKey(spkiBase64);
        return new Ed25519ResponseVerifier(publicKey);
    }

    static String normalizeAlgorithm(String algorithmHeader) {
        if (algorithmHeader == null || algorithmHeader.isBlank()) {
            return "ed25519";
        }
        return algorithmHeader.trim().toLowerCase();
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
