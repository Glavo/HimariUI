package org.glavo.himari.platform.api;

import org.jetbrains.annotations.NotNullByDefault;

/// Schedules UI work onto a platform-owned execution context without requiring a blocking pump.
///
/// Submission methods may be called from any thread. Callbacks execute serially on the owner thread
/// and callbacks with the same effective deadline execute in submission order. A host may drive
/// dispatch from an operating-system loop, a browser event turn, or a deterministic test harness.
@NotNullByDefault
public interface PlatformEventLoop {
    /// Returns the clock used to compare scheduled deadlines.
    ///
    /// @return this event loop's clock
    FrameClock clock();

    /// Returns whether the calling thread is the event-loop owner.
    ///
    /// @return whether the caller is the owner thread
    boolean isOwnerThread();

    /// Verifies that the calling thread is the event-loop owner.
    ///
    /// @throws IllegalStateException if called from another thread
    void checkOwnerThread();

    /// Schedules a callback for the next host dispatch opportunity.
    ///
    /// @param callback the callback to execute
    /// @return a handle that may cancel the callback before it begins
    /// @throws IllegalStateException if the loop no longer accepts work
    ScheduledTask post(Runnable callback);

    /// Schedules a callback at or after an absolute clock timestamp.
    ///
    /// A deadline at or before the current clock value becomes ready at the next host dispatch
    /// opportunity. Scheduling never blocks the caller.
    ///
    /// @param deadlineNanos the nonnegative absolute deadline in this loop's clock domain
    /// @param callback the callback to execute
    /// @return a handle that may cancel the callback before it begins
    /// @throws IllegalArgumentException if `deadlineNanos` is negative
    /// @throws IllegalStateException if the loop no longer accepts work
    ScheduledTask schedule(long deadlineNanos, Runnable callback);

    /// Returns whether the loop permanently stopped accepting callbacks.
    ///
    /// @return whether the loop is closed
    boolean isClosed();
}
