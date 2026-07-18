package net.opmasterleo.license.internal.platform;

import java.net.NetworkInterface;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.Enumeration;

public final class PlatformSupport {

    private static final int[] SHA256_K = {
            0x428a2f98, 0x71374491, 0xb5c0fbcf, 0xe9b5dba5, 0x3956c25b, 0x59f111f1, 0x923f82a4, 0xab1c5ed5,
            0xd807aa98, 0x12835b01, 0x243185be, 0x550c7dc3, 0x72be5d74, 0x80deb1fe, 0x9bdc06a7, 0xc19bf174,
            0xe49b69c1, 0xefbe4786, 0x0fc19dc6, 0x240ca1cc, 0x2de92c6f, 0x4a7484aa, 0x5cb0a9dc, 0x76f988da,
            0x983e5152, 0xa831c66d, 0xb00327c8, 0xbf597fc7, 0xc6e00bf3, 0xd5a79147, 0x06ca6351, 0x14292967,
            0x27b70a85, 0x2e1b2138, 0x4d2c6dfc, 0x53380d13, 0x650a7354, 0x766a0abb, 0x81c2c92e, 0x92722c85,
            0xa2bfe8a1, 0xa81a664b, 0xc24b8b70, 0xc76c51a3, 0xd192e819, 0xd6990624, 0xf40e3585, 0x106aa070,
            0x19a4c116, 0x1e376c08, 0x2748774c, 0x34b0bcb5, 0x391c0cb3, 0x4ed8aa4a, 0x5b9cca4f, 0x682e6ff3,
            0x748f82ee, 0x78a5636f, 0x84c87814, 0x8cc70208, 0x90befffa, 0xa4506ceb, 0xbef9a3f7, 0xc67178f2
    };

    private PlatformSupport() {
    }

    public static String readTextFile(String path) {
        return Checked.orNull(() -> {
            Path file = Path.of(path);
            if (!Files.exists(file)) {
                return null;
            }
            String content = Files.readString(file, StandardCharsets.UTF_8).trim();
            return content.isEmpty() ? null : content;
        });
    }

    public static String firstHardwareMac() {
        return Checked.orNull(() -> {
            Enumeration<NetworkInterface> networks = NetworkInterface.getNetworkInterfaces();
            while (networks.hasMoreElements()) {
                NetworkInterface network = networks.nextElement();
                byte[] hardwareAddress = network.getHardwareAddress();
                if (hardwareAddress == null || hardwareAddress.length == 0) {
                    continue;
                }
                StringBuilder builder = new StringBuilder();
                for (int i = 0; i < hardwareAddress.length; i++) {
                    builder.append(String.format("%02X", hardwareAddress[i]));
                    if (i + 1 < hardwareAddress.length) {
                        builder.append(':');
                    }
                }
                if (builder.length() > 0) {
                    return builder.toString();
                }
            }
            return null;
        });
    }

    public static String sha256HexPrefix(String value, int byteCount) {
        byte[] hash = sha256(value.getBytes(StandardCharsets.UTF_8));
        int limit = hash.length < byteCount ? hash.length : byteCount;
        StringBuilder out = new StringBuilder(limit * 2);
        for (int i = 0; i < limit; i++) {
            out.append(String.format("%02X", hash[i]));
        }
        return out.toString();
    }

    public static byte[] sha256(byte[] message) {
        byte[] jdk = Checked.orNull(() -> MessageDigest.getInstance("SHA-256").digest(message));
        if (jdk != null) {
            return jdk;
        }
        return sha256Pure(message);
    }

    private static byte[] sha256Pure(byte[] message) {
        int[] h = {
                0x6a09e667, 0xbb67ae85, 0x3c6ef372, 0xa54ff53a,
                0x510e527f, 0x9b05688c, 0x1f83d9ab, 0x5be0cd19
        };

        int length = message.length;
        long bitLen = ((long) length) * 8L;
        int paddedLength = ((length + 9 + 63) / 64) * 64;
        byte[] padded = new byte[paddedLength];
        System.arraycopy(message, 0, padded, 0, length);
        padded[length] = (byte) 0x80;
        for (int i = 0; i < 8; i++) {
            padded[paddedLength - 1 - i] = (byte) (bitLen >>> (8 * i));
        }

        int[] w = new int[64];
        for (int offset = 0; offset < paddedLength; offset += 64) {
            for (int i = 0; i < 16; i++) {
                int j = offset + i * 4;
                w[i] = ((padded[j] & 0xff) << 24)
                        | ((padded[j + 1] & 0xff) << 16)
                        | ((padded[j + 2] & 0xff) << 8)
                        | (padded[j + 3] & 0xff);
            }
            for (int i = 16; i < 64; i++) {
                int s0 = Integer.rotateRight(w[i - 15], 7) ^ Integer.rotateRight(w[i - 15], 18) ^ (w[i - 15] >>> 3);
                int s1 = Integer.rotateRight(w[i - 2], 17) ^ Integer.rotateRight(w[i - 2], 19) ^ (w[i - 2] >>> 10);
                w[i] = w[i - 16] + s0 + w[i - 7] + s1;
            }

            int a = h[0], b = h[1], c = h[2], d = h[3], e = h[4], f = h[5], g = h[6], hh = h[7];
            for (int i = 0; i < 64; i++) {
                int s1 = Integer.rotateRight(e, 6) ^ Integer.rotateRight(e, 11) ^ Integer.rotateRight(e, 25);
                int ch = (e & f) ^ ((~e) & g);
                int t1 = hh + s1 + ch + SHA256_K[i] + w[i];
                int s0 = Integer.rotateRight(a, 2) ^ Integer.rotateRight(a, 13) ^ Integer.rotateRight(a, 22);
                int maj = (a & b) ^ (a & c) ^ (b & c);
                int t2 = s0 + maj;
                hh = g;
                g = f;
                f = e;
                e = d + t1;
                d = c;
                c = b;
                b = a;
                a = t1 + t2;
            }
            h[0] += a;
            h[1] += b;
            h[2] += c;
            h[3] += d;
            h[4] += e;
            h[5] += f;
            h[6] += g;
            h[7] += hh;
        }

        byte[] out = new byte[32];
        for (int i = 0; i < 8; i++) {
            out[i * 4] = (byte) (h[i] >>> 24);
            out[i * 4 + 1] = (byte) (h[i] >>> 16);
            out[i * 4 + 2] = (byte) (h[i] >>> 8);
            out[i * 4 + 3] = (byte) h[i];
        }
        return out;
    }

    public static double parseDouble(String value, double fallback) {
        if (value == null || value.isEmpty()) {
            return fallback;
        }
        String trimmed = value.trim();
        if (trimmed.isEmpty()) {
            return fallback;
        }

        boolean negative = false;
        int index = 0;
        if (trimmed.charAt(0) == '-') {
            negative = true;
            index = 1;
        }
        if (index >= trimmed.length()) {
            return fallback;
        }

        long whole = 0;
        long fraction = 0;
        long fractionDivisor = 1;
        boolean inFraction = false;

        for (int i = index; i < trimmed.length(); i++) {
            char ch = trimmed.charAt(i);
            if (ch == '.') {
                if (inFraction) {
                    return fallback;
                }
                inFraction = true;
                continue;
            }
            if (ch < '0' || ch > '9') {
                return fallback;
            }
            int digit = ch - '0';
            if (!inFraction) {
                whole = (whole * 10L) + digit;
            } else {
                fraction = (fraction * 10L) + digit;
                fractionDivisor *= 10L;
            }
        }

        double result = whole + ((double) fraction / (double) fractionDivisor);
        if (negative) {
            result = -result;
        }
        return result;
    }

    public static Long parseLongOrNull(String value) {
        if (value == null || value.isEmpty()) {
            return null;
        }
        int index = 0;
        boolean negative = false;
        if (value.charAt(0) == '-') {
            negative = true;
            index = 1;
        }
        if (index >= value.length()) {
            return null;
        }
        long result = 0;
        for (int i = index; i < value.length(); i++) {
            char digit = value.charAt(i);
            if (digit < '0' || digit > '9') {
                return null;
            }
            result = (result * 10L) + (digit - '0');
        }
        if (negative) {
            return -result;
        }
        return result;
    }

    public static void sleep(long millis) {
        Checked.parkMillis(millis);
    }
}
