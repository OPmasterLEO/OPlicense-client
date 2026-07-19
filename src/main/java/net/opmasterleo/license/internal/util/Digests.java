package net.opmasterleo.license.internal.util;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

public final class Digests {

    private Digests() {
    }

    public static String sha256HexPrefix(String value, int byteCount) {
        byte[] hash = sha256(value.getBytes(StandardCharsets.UTF_8));
        int limit = Math.min(hash.length, byteCount);
        StringBuilder out = new StringBuilder(limit * 2);
        for (int i = 0; i < limit; i++) {
            out.append(String.format("%02X", hash[i] & 0xff));
        }
        return out.toString();
    }

    public static byte[] sha256(byte[] message) {
        try {
            return MessageDigest.getInstance("SHA-256").digest(message);
        } catch (Exception ignored) {
            return FallbackSha256.hash(message);
        }
    }
}
