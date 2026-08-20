package org.glavo.himari.layout.input.gesture;

import org.jetbrains.annotations.NotNullByDefault;

/// Clamps a scroll origin into the requested range with no overscroll.
@NotNullByDefault
public final class ClampingScrollPhysics implements ScrollPhysics {
    /// Shared default instance.
    public static final ClampingScrollPhysics INSTANCE = new ClampingScrollPhysics();

    /// Exponential fling friction in inverse seconds.
    public static final float FLING_FRICTION = 4.0f;

    /// Absolute velocity treated as rest, in units per second.
    public static final float FLING_REST_VELOCITY = 0.5f;

    /// Creates one clamp policy.
    public ClampingScrollPhysics() {
    }

    /// {@inheritDoc}
    @Override
    public int clampIndex(int index, int min, int max) {
        return Math.min(max, Math.max(min, index));
    }

    /// {@inheritDoc}
    @Override
    public float decayVelocity(float velocity, long elapsedNanos) {
        return decay(velocity, elapsedNanos);
    }

    /// Applies exponential friction to `velocity`.
    ///
    /// @param velocity the velocity in units per second
    /// @param elapsedNanos the nonnegative sample duration
    /// @return the remaining velocity, or `0` when settled
    public static float decay(float velocity, long elapsedNanos) {
        if (!Float.isFinite(velocity)) {
            throw new IllegalArgumentException("Fling velocity must be finite");
        }
        if (elapsedNanos < 0L) {
            throw new IllegalArgumentException("elapsedNanos must be nonnegative");
        }
        if (elapsedNanos == 0L || velocity == 0.0f) {
            return velocity;
        }
        double dt = elapsedNanos / 1_000_000_000.0;
        float next = (float) (velocity * Math.exp(-FLING_FRICTION * dt));
        return Math.abs(next) < FLING_REST_VELOCITY ? 0.0f : next;
    }
}
