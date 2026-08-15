package org.glavo.himari.ffi;

import org.jetbrains.annotations.NotNullByDefault;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Unmodifiable;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ConcurrentLinkedQueue;

/// Stores failures contained by generated native callback adapters in publication order.
///
/// All operations are thread-safe. Polling or draining removes the returned failures from this queue.
@NotNullByDefault
public final class CallbackFailureQueue implements CallbackFailureSink {
    /// The lock-free failure queue.
    private final ConcurrentLinkedQueue<Throwable> failures = new ConcurrentLinkedQueue<>();

    /// Creates an empty callback failure queue.
    public CallbackFailureQueue() {
    }

    /// Publishes one contained callback failure.
    ///
    /// @param failure the non-null failure to enqueue
    @Override
    public void publish(Throwable failure) {
        failures.add(Objects.requireNonNull(failure, "failure"));
    }

    /// Removes and returns the earliest queued failure.
    ///
    /// @return the earliest failure, or `null` when the queue is empty
    public @Nullable Throwable poll() {
        return failures.poll();
    }

    /// Removes and returns every failure currently reachable from the queue.
    ///
    /// Failures published concurrently may appear in this result or remain queued for a later drain.
    ///
    /// @return an immutable list in queue order
    public @Unmodifiable List<Throwable> drain() {
        List<Throwable> drained = new ArrayList<>();
        @Nullable Throwable failure;
        while ((failure = failures.poll()) != null) {
            drained.add(failure);
        }
        return List.copyOf(drained);
    }

    /// Returns whether no failure is currently observable.
    ///
    /// @return whether the queue is empty at the time of the call
    public boolean isEmpty() {
        return failures.isEmpty();
    }

    /// Returns the current estimated number of queued failures.
    ///
    /// The traversal is linear and may observe concurrent publication or removal.
    ///
    /// @return the observed queue size
    public int size() {
        return failures.size();
    }
}
