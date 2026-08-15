package org.glavo.himari.platform.headless;

import org.glavo.himari.platform.api.FrameClock;
import org.jetbrains.annotations.NotNullByDefault;

/// Implements a monotonic frame clock advanced explicitly by its owner thread without sleeping.
///
/// Reads are safe from any thread. Advancement is confined to the thread that creates the clock so
/// one test driver defines a deterministic timestamp sequence.
@NotNullByDefault
public final class ManualFrameClock implements FrameClock {
    /// The thread permitted to advance this clock.
    private final Thread ownerThread;

    /// The current nonnegative timestamp, published for cross-thread reads.
    private volatile long nowNanos;

    /// Creates a clock at timestamp zero owned by the current thread.
    public ManualFrameClock() {
        this(0L);
    }

    /// Creates a clock at a specified timestamp owned by the current thread.
    ///
    /// @param initialNanos the nonnegative initial timestamp
    /// @throws IllegalArgumentException if `initialNanos` is negative
    public ManualFrameClock(long initialNanos) {
        if (initialNanos < 0L) {
            throw new IllegalArgumentException("Initial timestamp must be nonnegative");
        }
        this.ownerThread = Thread.currentThread();
        this.nowNanos = initialNanos;
    }

    /// Returns the current timestamp.
    ///
    /// @return the nonnegative current timestamp in nanoseconds
    @Override
    public long nowNanos() {
        return nowNanos;
    }

    /// Returns whether the calling thread owns clock advancement.
    ///
    /// @return whether the caller is the owner thread
    public boolean isOwnerThread() {
        return Thread.currentThread() == ownerThread;
    }

    /// Verifies that the calling thread owns clock advancement.
    ///
    /// @throws IllegalStateException if called from another thread
    public void checkOwnerThread() {
        if (!isOwnerThread()) {
            throw new IllegalStateException(
                    "Manual clock is owned by thread '" + ownerThread.getName()
                            + "' but was accessed from '" + Thread.currentThread().getName() + "'"
            );
        }
    }

    /// Advances the clock to an absolute timestamp.
    ///
    /// Advancing does not itself dispatch event-loop work.
    ///
    /// @param timestampNanos the new timestamp, no earlier than the current value
    /// @throws IllegalArgumentException if the timestamp moves backwards
    /// @throws IllegalStateException if called from another thread
    public void advanceTo(long timestampNanos) {
        checkOwnerThread();
        if (timestampNanos < nowNanos) {
            throw new IllegalArgumentException("A manual clock cannot move backwards");
        }
        nowNanos = timestampNanos;
    }

    /// Advances the clock by a nonnegative duration.
    ///
    /// @param deltaNanos the nonnegative duration in nanoseconds
    /// @throws IllegalArgumentException if `deltaNanos` is negative
    /// @throws ArithmeticException if the resulting timestamp exceeds `long` range
    /// @throws IllegalStateException if called from another thread
    public void advanceBy(long deltaNanos) {
        checkOwnerThread();
        if (deltaNanos < 0L) {
            throw new IllegalArgumentException("Clock advance must be nonnegative");
        }
        advanceTo(Math.addExact(nowNanos, deltaNanos));
    }
}
