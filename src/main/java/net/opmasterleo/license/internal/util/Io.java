package net.opmasterleo.license.internal.util;

import java.io.Closeable;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

public final class Io {

    private Io() {
    }

    public static String readLimitedUtf8(InputStream stream, int maxBytes) throws Exception {
        byte[] buffer = new byte[4096];
        byte[] collected = new byte[Math.min(maxBytes, 8192)];
        int size = 0;
        int read = stream.read(buffer);
        while (read != -1) {
            if (size + read > maxBytes) {
                throw new Exception("Response body exceeds " + maxBytes + " bytes");
            }
            if (size + read > collected.length) {
                int next = Math.min(maxBytes, Math.max(collected.length * 2, size + read));
                byte[] grown = new byte[next];
                System.arraycopy(collected, 0, grown, 0, size);
                collected = grown;
            }
            System.arraycopy(buffer, 0, collected, size, read);
            size += read;
            read = stream.read(buffer);
        }
        return new String(collected, 0, size, StandardCharsets.UTF_8);
    }

    public static void closeQuietly(Closeable closeable) {
        if (closeable == null) {
            return;
        }
        try {
            closeable.close();
        } catch (Exception ignored) {
        }
    }
}
