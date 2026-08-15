package org.glavo.himari.platform.windows;

import org.glavo.himari.platform.api.FrameClock;
import org.jetbrains.annotations.NotNullByDefault;

/// Supplies monotonic timestamps from `System.nanoTime()`.
@NotNullByDefault
public final class WindowsFrameClock implements FrameClock {
    /// The origin used so returned values stay nonnegative.
    private final long originNanos;

    /// Creates a clock whose first reading is zero or greater.
    public WindowsFrameClock() {
        this.originNanos = System.nanoTime();
    }

    /// Returns the elapsed monotonic time since construction.
    ///
    /// @return the nonnegative timestamp
    @Override
    public long nowNanos() {
        long elapsed = System.nanoTime() - originNanos;
        return elapsed < 0L ? 0L : elapsed;
    }
}
