package org.glavo.himari.runtime.animation;

import org.jetbrains.annotations.NotNullByDefault;

import java.util.Objects;

/// Evolves a scalar through a cubic Bézier unit curve over a fixed monotonic duration.
///
/// @param durationNanos the positive active duration in nanoseconds
/// @param delayNanos the nonnegative delay before active progress begins
/// @param curve the deterministic unit curve
@NotNullByDefault
public record TweenSpec(long durationNanos, long delayNanos, CubicBezierCurve curve)
        implements MotionSpec {
    /// Creates a validated tween specification.
    ///
    /// @throws IllegalArgumentException if duration is not positive, delay is negative, or their sum
    /// exceeds the nanosecond range
    public TweenSpec {
        if (durationNanos <= 0L) {
            throw new IllegalArgumentException("durationNanos must be positive");
        }
        if (delayNanos < 0L) {
            throw new IllegalArgumentException("delayNanos must be non-negative");
        }
        if (Long.MAX_VALUE - delayNanos < durationNanos) {
            throw new IllegalArgumentException("Tween delay and duration exceed nanosecond range");
        }
        Objects.requireNonNull(curve, "curve");
    }

    /// Creates a zero-delay linear tween.
    ///
    /// @param durationNanos the positive duration
    /// @return the tween specification
    /// @throws IllegalArgumentException if `durationNanos` is not positive
    public static TweenSpec linear(long durationNanos) {
        return new TweenSpec(durationNanos, 0L, CubicBezierCurve.LINEAR);
    }

    /// Creates a zero-delay symmetric ease-in-out tween.
    ///
    /// @param durationNanos the positive duration
    /// @return the tween specification
    /// @throws IllegalArgumentException if `durationNanos` is not positive
    public static TweenSpec easeInOut(long durationNanos) {
        return new TweenSpec(durationNanos, 0L, CubicBezierCurve.EASE_IN_OUT);
    }

    /// Returns that a tween has an active timeline.
    ///
    /// @return `false`
    @Override
    public boolean isImmediate() {
        return false;
    }

    /// Returns that replacement preserves value but not tween velocity.
    ///
    /// @return `false`
    @Override
    public boolean supportsVelocityRetargeting() {
        return false;
    }
}
