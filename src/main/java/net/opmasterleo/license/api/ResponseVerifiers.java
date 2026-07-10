package net.opmasterleo.license.api;

import net.opmasterleo.license.internal.crypto.Ed25519ResponseVerifier;

public final class ResponseVerifiers {

    private ResponseVerifiers() {
    }

    public static ResponseVerifier ed25519(String spkiBase64) {
        return new Ed25519ResponseVerifier(spkiBase64);
    }
}
