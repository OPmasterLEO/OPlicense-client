package net.opmasterleo.license.internal.security;

import java.security.SecureRandom;
import java.util.Base64;

public final class Nonce {

    private static final SecureRandom RANDOM = new SecureRandom();

    private Nonce() {
    }

    public static String create() {
        byte[] raw = new byte[24];
        RANDOM.nextBytes(raw);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(raw);
    }
}
