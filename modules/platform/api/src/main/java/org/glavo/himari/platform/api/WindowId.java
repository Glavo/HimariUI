package org.glavo.himari.platform.api;

import org.jetbrains.annotations.NotNullByDefault;

/// Identifies one window within a platform session without exposing a target handle.
///
/// @param value the positive session-local identifier
@NotNullByDefault
public record WindowId(long value) implements Comparable<WindowId> {
    /// Creates a window identifier.
    ///
    /// @throws IllegalArgumentException if `value` is not positive
    public WindowId {
        if (value <= 0L) {
            throw new IllegalArgumentException("Window identifier must be positive");
        }
    }

    /// Compares identifiers by their unsigned-free positive numeric values.
    ///
    /// @param other the other identifier
    /// @return the comparison result
    @Override
    public int compareTo(WindowId other) {
        return Long.compare(value, other.value);
    }
}
