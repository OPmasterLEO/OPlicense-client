package net.opmasterleo.license.internal.crypto;

import net.opmasterleo.license.api.ResponseVerifier;

import java.security.PublicKey;

public final class Ed25519ResponseVerifier implements ResponseVerifier {

    private final PublicKey publicKey;

    public Ed25519ResponseVerifier(PublicKey publicKey) {
        this.publicKey = publicKey;
    }

    public Ed25519ResponseVerifier(String spkiBase64) {
        this(Ed25519.decodePublicKey(spkiBase64));
    }

    @Override
    public boolean verify(String payload, String signature, String algorithmHeader) {
        String algorithm = algorithmHeader;
        if (algorithm == null || algorithm.isBlank()) {
            algorithm = "ed25519";
        } else {
            algorithm = algorithm.trim().toLowerCase();
        }
        if (!"ed25519".equals(algorithm)) {
            return false;
        }
        return Ed25519.verify(payload, signature, publicKey);
    }
}
