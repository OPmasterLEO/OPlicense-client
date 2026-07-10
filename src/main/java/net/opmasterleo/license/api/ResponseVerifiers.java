package net.opmasterleo.license.api;

public final class ResponseVerifiers {

    private ResponseVerifiers() {
    }

    public static Ed25519ResponseVerifier createEd25519(String spkiBase64) {
        return new Ed25519ResponseVerifier(spkiBase64);
    }
}
