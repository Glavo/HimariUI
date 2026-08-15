package org.glavo.himari.runtime.effect;

import org.jetbrains.annotations.NotNullByDefault;

import java.util.concurrent.Future;

/// Cancels one asynchronous effect unit of work.
@NotNullByDefault
public final class AsyncEffectHandle {
    /// The worker future representing the launched work.
    private final Future<?> future;

    /// Creates one handle.
    ///
    /// @param future the launched work
    AsyncEffectHandle(Future<?> future) {
        this.future = future;
    }

    /// Requests cooperative cancellation and interrupts the worker when it is running.
    public void cancel() {
        future.cancel(true);
    }

    /// Returns whether the work completed, failed, or was cancelled.
    ///
    /// @return whether the work is no longer running
    public boolean isDone() {
        return future.isDone();
    }

    /// Returns whether cancellation was requested.
    ///
    /// @return whether the work was cancelled
    public boolean isCancelled() {
        return future.isCancelled();
    }
}
