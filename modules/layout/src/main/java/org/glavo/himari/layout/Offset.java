package org.glavo.himari.layout;

import org.jetbrains.annotations.NotNullByDefault;

/// Stores a placement origin in logical pixels.
///
/// @param x the horizontal origin
/// @param y the vertical origin
@NotNullByDefault
public record Offset(float x, float y) {
    /// The origin.
    public static final Offset ZERO = new Offset(0.0f, 0.0f);

    /// Validates the offset.
    public Offset {
        if (!Float.isFinite(x) || !Float.isFinite(y)) {
            throw new IllegalArgumentException("Offset values must be finite");
        }
    }

    /// Returns the vector sum of this offset and another offset.
    ///
    /// @param other the addend
    /// @return the sum
    public Offset plus(Offset other) {
        return new Offset(x + other.x, y + other.y);
    }
}
