package org.glavo.himari.platform.api;

import org.jetbrains.annotations.NotNullByDefault;

/// Supplies monotonically nondecreasing timestamps for frame and host-event work.
///
/// Timestamps use nanoseconds in an implementation-defined epoch. Only differences between values
/// obtained from the same clock are meaningful. Implementations must not use wall-clock time or
/// adjust timestamps when the civil clock changes.
@NotNullByDefault
public interface FrameClock {
    /// Returns the current monotonic timestamp.
    ///
    /// @return a nonnegative timestamp in nanoseconds
    long nowNanos();
}
