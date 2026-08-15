package org.glavo.himari.runtime.effect;

import org.jetbrains.annotations.NotNullByDefault;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ExecutorService;

/// Provides owner-thread services to one mounted or updating effect.
@NotNullByDefault
public final class EffectSession {
    /// The worker pool used for asynchronous work.
    private final ExecutorService workers;

    /// Handles launched by this session.
    private final ArrayList<AsyncEffectHandle> launched = new ArrayList<>();

    /// Whether this session still accepts launches.
    private boolean open = true;

    /// Creates one session.
    ///
    /// @param workers the shared worker pool
    EffectSession(ExecutorService workers) {
        this.workers = Objects.requireNonNull(workers, "workers");
    }

    /// Launches one asynchronous unit of work on a virtual worker thread.
    ///
    /// The work must not touch UI objects. Cancellation interrupts the worker and is requested
    /// automatically when the owning effect is cleaned up.
    ///
    /// @param work the worker body
    /// @return a handle that can cancel the work
    public AsyncEffectHandle launch(Runnable work) {
        Objects.requireNonNull(work, "work");
        if (!open) {
            throw new IllegalStateException("Effect session is closed");
        }
        AsyncEffectHandle handle = new AsyncEffectHandle(workers.submit(work));
        launched.add(handle);
        return handle;
    }

    /// Returns the handles launched by this session.
    ///
    /// @return the launched handles
    List<AsyncEffectHandle> launched() {
        return launched;
    }

    /// Prevents further launches after the callback returns.
    void close() {
        open = false;
    }
}
