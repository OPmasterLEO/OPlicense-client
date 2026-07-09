package net.opmasterleo.license.internal.crypto;

import net.opmasterleo.license.internal.platform.PlatformSupport;

import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.Signature;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

public final class Ed25519 {

    private Ed25519() {
    }

    public static PublicKey decodePublicKey(String spkiBase64) {
        try {
            String normalized = normalizePublicKey(spkiBase64);
            byte[] der = Base64.getDecoder().decode(normalized);
            KeyFactory factory = KeyFactory.getInstance("Ed25519");
            return factory.generatePublic(new X509EncodedKeySpec(der));
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid Ed25519 public key", e);
        }
    }

    public static boolean verify(String payload, String signatureBase64, PublicKey publicKey) {
        if (payload == null || signatureBase64 == null || publicKey == null) {
            return false;
        }
        Boolean verified = PlatformSupport.call(() -> {
            Signature signature = Signature.getInstance("Ed25519");
            signature.initVerify(publicKey);
            signature.update(payload.getBytes(StandardCharsets.UTF_8));
            return signature.verify(Base64.getDecoder().decode(signatureBase64.trim()));
        }, Boolean.FALSE);
        return Boolean.TRUE.equals(verified);
    }

    private static String normalizePublicKey(String value) {
        if (value == null) {
            return "";
        }

        String normalized = value.trim();
        if (normalized.startsWith("`") && normalized.endsWith("`") && normalized.length() >= 2) {
            normalized = normalized.substring(1, normalized.length() - 1).trim();
        }

        normalized = normalized
                .replace("-----BEGIN PUBLIC KEY-----", "")
                .replace("-----END PUBLIC KEY-----", "")
                .replaceAll("\\s+", "");

        return normalized;
    }
}
