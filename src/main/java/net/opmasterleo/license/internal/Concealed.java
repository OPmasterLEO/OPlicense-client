package net.opmasterleo.license.internal;

/**
 * Runtime string concealment. Not unbreakable — only raises the bar over plain literals.
 * Generate arrays with {@link #encode(String, int)} during development, then delete call sites.
 */
public final class Concealed {

    private Concealed() {
    }

    public static String decode(int[] encoded, int seed) {
        char[] chars = new char[encoded.length];
        for (int i = 0; i < encoded.length; i++) {
            chars[i] = (char) (encoded[i] ^ mix(seed, i));
        }
        return new String(chars);
    }

    public static int[] encode(String value, int seed) {
        int[] encoded = new int[value.length()];
        for (int i = 0; i < value.length(); i++) {
            encoded[i] = value.charAt(i) ^ mix(seed, i);
        }
        return encoded;
    }

    private static int mix(int seed, int index) {
        int x = seed ^ (index * 0x9E3779B9);
        x ^= (x >>> 16);
        x *= 0x85EBCA6B;
        x ^= (x >>> 13);
        return x & 0xFFFF;
    }
}
