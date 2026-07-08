package net.opmasterleo.license.internal;

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
            byte[] der = Base64.getDecoder().decode(spkiBase64);
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
        try {
            Signature signature = Signature.getInstance("Ed25519");
            signature.initVerify(publicKey);
            signature.update(payload.getBytes(StandardCharsets.UTF_8));
            return signature.verify(Base64.getDecoder().decode(signatureBase64.trim()));
        } catch (Exception e) {
            return false;
        }
    }
}
