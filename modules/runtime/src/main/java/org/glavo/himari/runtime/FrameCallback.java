package org.glavo.himari.runtime;

import org.jetbrains.annotations.NotNullByDefault;

/// Executes one window-scoped UI frame on the platform event-loop owner context.
///
/// Implementations must perform bounded synchronous work and must not block the owner context.
/// A thrown exception or error is contained by [WindowFrameScheduler] and recorded as a
/// [UiSchedulerFailure].
@FunctionalInterface
@NotNullByDefault
public interface FrameCallback {
    /// Executes one frame using the timestamp sampled for the complete attempt.
    ///
    /// @param tick the immutable frame timing and coalescing record
    void runFrame(FrameTick tick);
}
