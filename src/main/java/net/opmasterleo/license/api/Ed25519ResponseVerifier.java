package net.opmasterleo.license.api;

import net.opmasterleo.license.internal.security.Ed25519;

import java.security.PublicKey;

/** Verifies Ed25519-signed license API responses. */
public final class Ed25519ResponseVerifier {

    private final PublicKey publicKey;

    /** Creates a verifier from a decoded public key. */
    public Ed25519ResponseVerifier(PublicKey publicKey) {
        if (publicKey == null) {
            throw new IllegalArgumentException("publicKey is required");
        }
        this.publicKey = publicKey;
    }

    /** Creates a verifier from an SPKI Base64 public key. */
    public Ed25519ResponseVerifier(String spkiBase64) {
        this(Ed25519.decodePublicKey(spkiBase64));
    }

    /** Creates a verifier from an SPKI Base64 public key. */
    public static Ed25519ResponseVerifier createEd25519(String spkiBase64) {
        return new Ed25519ResponseVerifier(spkiBase64);
    }

    /** Returns {@code true} when the payload signature is valid. */
    public boolean verify(String payload, String signature, String algorithmHeader) {
        if (payload == null || signature == null) {
            return false;
        }
        String trimmedSignature = signature.trim();
        if (trimmedSignature.isEmpty()) {
            return false;
        }
        String algorithm = algorithmHeader;
        if (algorithm == null || algorithm.isBlank()) {
            algorithm = "ed25519";
        } else {
            algorithm = algorithm.trim().toLowerCase();
        }
        if (!"ed25519".equals(algorithm)) {
            return false;
        }
        return Ed25519.verify(payload, trimmedSignature, publicKey);
    }
}
