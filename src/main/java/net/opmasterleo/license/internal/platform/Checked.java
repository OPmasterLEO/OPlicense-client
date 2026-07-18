package net.opmasterleo.license.internal.platform;

import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;
import java.util.concurrent.locks.LockSupport;

public final class Checked {

    @FunctionalInterface
    public interface Call<T> {
        T run() throws Exception;
    }

    private Checked() {
    }

    public static <T> T orNull(Call<T> call) {
        FutureTask<T> task = new FutureTask<>(call::run);
        task.run();
        if (task.state() == Future.State.SUCCESS) {
            return task.resultNow();
        }
        return null;
    }

    public static <T> T orElse(Call<T> call, T fallback) {
        T value = orNull(call);
        return value != null ? value : fallback;
    }

    public static boolean ofBoolean(Call<Boolean> call, boolean fallback) {
        Boolean value = orNull(call);
        return value != null ? value.booleanValue() : fallback;
    }

    public static void parkMillis(long millis) {
        if (millis <= 0L) {
            return;
        }
        LockSupport.parkNanos(millis * 1_000_000L);
    }
}
