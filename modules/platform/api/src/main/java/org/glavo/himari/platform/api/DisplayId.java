package org.glavo.himari.platform.api;

import org.jetbrains.annotations.NotNullByDefault;

/// Identifies one display within a platform session without exposing a target handle.
///
/// @param value the stable, nonblank session-local identifier
@NotNullByDefault
public record DisplayId(String value) implements Comparable<DisplayId> {
    /// Creates a display identifier.
    ///
    /// @throws IllegalArgumentException if `value` is blank or has surrounding whitespace
    public DisplayId {
        if (value.isBlank() || !value.equals(value.strip())) {
            throw new IllegalArgumentException("Display identifier must be nonblank and trimmed");
        }
    }

    /// Compares identifiers lexicographically by their stable values.
    ///
    /// @param other the other identifier
    /// @return the comparison result
    @Override
    public int compareTo(DisplayId other) {
        return value.compareTo(other.value);
    }

    /// Returns the stable identifier value.
    ///
    /// @return the identifier value
    @Override
    public String toString() {
        return value;
    }
}
