package org.glavo.himari.platform.windows;

import org.glavo.himari.platform.api.ScheduledTask;
import org.glavo.himari.platform.api.ScheduledTaskState;
import org.jetbrains.annotations.NotNullByDefault;

import java.util.Objects;

/// Stores one posted or delayed Windows event-loop callback.
@NotNullByDefault
final class WindowsScheduledTask implements ScheduledTask {
    /// The owning loop.
    private final WindowsEventLoop loop;

    /// The absolute deadline.
    private final long deadlineNanos;

    /// The submission sequence.
    private final long sequence;

    /// The callback.
    private final Runnable callback;

    /// The current state, guarded by the loop lock.
    private ScheduledTaskState state = ScheduledTaskState.PENDING;

    /// Creates one task.
    ///
    /// @param loop the owner
    /// @param deadlineNanos the deadline
    /// @param sequence the sequence
    /// @param callback the callback
    WindowsScheduledTask(WindowsEventLoop loop, long deadlineNanos, long sequence, Runnable callback) {
        this.loop = Objects.requireNonNull(loop, "loop");
        this.deadlineNanos = deadlineNanos;
        this.sequence = sequence;
        this.callback = Objects.requireNonNull(callback, "callback");
    }

    /// {@inheritDoc}
    @Override
    public boolean cancel() {
        return loop.cancel(this);
    }

    /// {@inheritDoc}
    @Override
    public ScheduledTaskState state() {
        return loop.stateOf(this);
    }

    /// Returns the deadline.
    ///
    /// @return the deadline
    long deadlineNanos() {
        return deadlineNanos;
    }

    /// Returns the sequence.
    ///
    /// @return the sequence
    long sequence() {
        return sequence;
    }

    /// Returns the callback.
    ///
    /// @return the callback
    Runnable callback() {
        return callback;
    }

    /// Returns the state while the loop lock is held.
    ///
    /// @return the state
    ScheduledTaskState stateUnderLock() {
        return state;
    }

    /// Marks the task cancelled while the loop lock is held.
    void cancelUnderLock() {
        if (state == ScheduledTaskState.PENDING) {
            state = ScheduledTaskState.CANCELLED;
        }
    }

    /// Marks the task completed.
    void complete() {
        state = ScheduledTaskState.COMPLETED;
    }

    /// Installs a replacement state while the loop lock is held.
    ///
    /// @param next the next state
    void setStateUnderLock(ScheduledTaskState next) {
        state = next;
    }
}
