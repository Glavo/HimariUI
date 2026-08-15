package org.glavo.himari.layout.input.gesture;

import org.jetbrains.annotations.NotNullByDefault;

/// Estimates pointer velocity from successive timestamped samples.
@NotNullByDefault
final class VelocityTracker {
    /// Horizontal sample at the previous add, or `NaN` before the first sample.
    private float previousX = Float.NaN;

    /// Vertical sample at the previous add.
    private float previousY;

    /// Timestamp of the previous add.
    private long previousNanos;

    /// Latest estimated velocity.
    private GestureVelocity velocity = GestureVelocity.zero();

    /// Creates an empty tracker.
    VelocityTracker() {
    }

    /// Drops every stored sample.
    void clear() {
        previousX = Float.NaN;
        previousY = 0.0f;
        previousNanos = 0L;
        velocity = GestureVelocity.zero();
    }

    /// Records one sample and updates the estimated velocity.
    ///
    /// @param x the horizontal position
    /// @param y the vertical position
    /// @param timestampNanos the sample timestamp
    void add(float x, float y, long timestampNanos) {
        if (Float.isFinite(previousX) && timestampNanos > previousNanos) {
            double seconds = (timestampNanos - previousNanos) / 1_000_000_000.0;
            velocity = new GestureVelocity(
                    (float) ((x - previousX) / seconds),
                    (float) ((y - previousY) / seconds)
            );
        }
        previousX = x;
        previousY = y;
        previousNanos = timestampNanos;
    }

    /// Returns the latest estimate, or zero when fewer than two samples exist.
    ///
    /// @return the velocity
    GestureVelocity velocity() {
        return velocity;
    }
}
