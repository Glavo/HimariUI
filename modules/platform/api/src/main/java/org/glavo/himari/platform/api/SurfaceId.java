package org.glavo.himari.platform.api;

import org.jetbrains.annotations.NotNullByDefault;

/// Identifies one presentation surface without exposing its target-specific representation.
///
/// @param value the positive session-local identifier
@NotNullByDefault
public record SurfaceId(long value) {
    /// Creates a surface identifier.
    ///
    /// @throws IllegalArgumentException if `value` is not positive
    public SurfaceId {
        if (value <= 0L) {
            throw new IllegalArgumentException("Surface identifier must be positive");
        }
    }
}
