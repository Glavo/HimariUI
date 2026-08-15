package org.glavo.himari.layout.input.gesture;

import org.jetbrains.annotations.NotNullByDefault;

/// Stores one pointer velocity in logical pixels per second.
///
/// @param x the horizontal velocity
/// @param y the vertical velocity
@NotNullByDefault
public record GestureVelocity(float x, float y) {
    /// Validates finite components.
    public GestureVelocity {
        if (!Float.isFinite(x) || !Float.isFinite(y)) {
            throw new IllegalArgumentException("Velocity components must be finite");
        }
    }

    /// Returns a zero velocity.
    ///
    /// @return the zero velocity
    public static GestureVelocity zero() {
        return new GestureVelocity(0.0f, 0.0f);
    }
}
