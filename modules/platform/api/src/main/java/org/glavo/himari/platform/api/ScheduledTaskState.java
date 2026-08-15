package org.glavo.himari.platform.api;

import org.jetbrains.annotations.NotNullByDefault;

/// Identifies the lifecycle state of a task submitted to a [PlatformEventLoop].
@NotNullByDefault
public enum ScheduledTaskState {
    /// The task is waiting for its deadline and may still be cancelled.
    PENDING,

    /// The task callback is currently executing on the event-loop owner thread.
    RUNNING,

    /// The task callback returned or threw an exception.
    COMPLETED,

    /// The task was cancelled before callback execution began.
    CANCELLED
}
