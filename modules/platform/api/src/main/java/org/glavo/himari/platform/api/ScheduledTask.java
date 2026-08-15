package org.glavo.himari.platform.api;

import org.jetbrains.annotations.NotNullByDefault;

/// Controls one callback submitted to a [PlatformEventLoop].
@NotNullByDefault
public interface ScheduledTask {
    /// Cancels the callback if it has not begun executing.
    ///
    /// This method may be called from any thread. Cancellation does not interrupt a callback that
    /// is already running.
    ///
    /// @return `true` if this call changed the state from [ScheduledTaskState#PENDING] to
    /// [ScheduledTaskState#CANCELLED], otherwise `false`
    boolean cancel();

    /// Returns the current lifecycle state.
    ///
    /// @return the current state
    ScheduledTaskState state();

    /// Returns whether the callback can no longer begin execution.
    ///
    /// @return `true` for completed or cancelled tasks
    default boolean isDone() {
        ScheduledTaskState current = state();
        return current == ScheduledTaskState.COMPLETED || current == ScheduledTaskState.CANCELLED;
    }
}
