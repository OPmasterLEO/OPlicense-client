package net.opmasterleo.license.internal;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.security.NoSuchAlgorithmException;
import java.security.InvalidKeyException;

public final class Hmac {

    private Hmac() {
    }

    public static String sign(String payload, String secret) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] raw = mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder();
            for (byte b : raw) {
                hex.append(String.format("%02x", b));
            }
            return hex.toString();
        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            throw new RuntimeException(e);
        }
    }

    public static boolean verify(String payload, String secret, String signature) {
        if (signature == null) return false;
        String normalizedSignature = signature.toLowerCase(Locale.ROOT);
        String expected = sign(payload, secret);
        if (expected.length() != normalizedSignature.length()) return false;
        int diff = 0;
        for (int i = 0; i < expected.length(); i++) {
            diff |= expected.charAt(i) ^ normalizedSignature.charAt(i);
        }
        return diff == 0;
    }
}
